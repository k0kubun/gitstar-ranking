class UserUpdateJob < ActiveJob::Base
  BATCH_SIZE = 100
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

  def perform(user_id)
    return unless Repository.owned_by?(user_id)

    start    = Time.now
    all_rows = all_repos(user_id)
    star     = 0

    all_rows.each_slice(BATCH_SIZE) do |rows|
      repos = []

      rows.each do |row|
        repo = Repository.new
        repo.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES)
        repo.owner_id   = row[:owner] && row[:owner][:id]
        repo.fetched_at = start
        repos << repo

        star += row[:stargazers_count]
      end

      Repository.import(repos)
    end

    User.where(id: user_id).limit(1).update_all(stargazers_count: star)
    Github::LimitBalancer.instance.log_limit
    logger.info "Updated #{all_rows.size} repos for #{user_id}: #{Time.now - start}s"
  rescue => e
    logger.error "#{user_id}: #{e.class}: #{e}"
  end

  private

  def all_repos(user_id)
    client = Github::LimitBalancer.instance.client
    client.auto_paginate = true
    client.repos(user_id)
  end

  def logger
    @logger ||= Logger.new('log/user_update_job.log')
  end
end
