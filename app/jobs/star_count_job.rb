class StarCountJob < ActiveJob::Base
  CONCURRENCY = 10
  FETCH_ATTRIBUTES = %i[
    id
    name
    full_name
    description
    fork
    homepage
    stargazers_count
    language
  ]

  queue_as :default

  def perform(user_ids)
    range    = user_ids.size
    start    = Time.now
    user_ids = filter_user_ids(user_ids)

    rows_by_user_id = Hash.new { |h, k| h[k] = [] }
    user_ids.each_slice(CONCURRENCY) do |ids|
      Parallel.each(ids, in_threads: ids.size) do |id|
        rows_by_user_id[id] = all_repos(id)
      end
    end

    users = []
    repos = []
    user_ids.each do |user_id|
      rows = rows_by_user_id[user_id]
      user, counted_repos = count_stars_for(rows, user_id)

      users << user
      repos += counted_repos
    end

    User.import(users)
    Repository.import(repos)

    logger.info "Updated #{repos.size} repos for #{range}(#{user_ids.size}) users: #{Time.now - start}s"
  rescue => e
    logger.error "#{user_ids}: #{e.class}: #{e}"
  end

  private

  def count_stars_for(rows, user_id)
    repos = []
    star  = 0

    rows.each do |row|
      repo = Repository.new
      repo.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES)
      repo.owner_id   = row[:owner] && row[:owner][:id]
      repo.fetched_at = Time.now
      repos << repo

      star += row[:stargazers_count]
    end

    user = User.new(id: user_id, stargazers_count: star)
    [user, repos]
  end

  def filter_user_ids(user_ids)
    Repository.where(owner_id: user_ids).uniq.pluck(:owner_id)
  end

  def all_repos(user_id)
    client = Github::LimitBalancer.instance.client
    client.auto_paginate = true
    client.repos(user_id)
  end

  def logger
    @logger ||= Logger.new('log/star_count_job.log')
  end
end
