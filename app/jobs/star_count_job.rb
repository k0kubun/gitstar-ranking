class StarCountJob < ActiveJob::Base
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
    start    = Time.now
    user_ids = filter_user_ids(user_ids)

    users = []
    repos = []
    user_ids.each do |user_id|
      user, counted_repos = count_stars_for(user_id)
      next if user.blank? || counted_repos.blank?

      users << user
      repos += counted_repos
    end

    User.import(users)
    Repository.import(repos)

    logger.info "Updated #{repos.size} repos for #{user_ids.size} users: #{Time.now - start}s"
  rescue => e
    logger.error "#{user_ids}: #{e.class}: #{e}"
  end

  private

  def count_stars_for(user_id)
    repos = []
    star  = 0

    all_repos(user_id).each do |row|
      repo = Repository.new
      repo.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES)
      repo.owner_id   = row[:owner] && row[:owner][:id]
      repo.fetched_at = Time.now
      repos << repo

      star += row[:stargazers_count]
    end

    user = User.new(id: user_id, stargazers_count: star)
    [user, repos]
  rescue => e
    logger.error "#{user_id}: #{e.class}: #{e}"
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
    @logger ||= Logger.new('log/user_update_job.log')
  end
end
