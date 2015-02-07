class UserFetchJob < ActiveJob::Base
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
    start    = Time.now
    all_rows = all_repos(user_id)
    star     = 0

    repos = []
    all_rows.each do |row|
      repo = []
      FETCH_ATTRIBUTES.each do |attribute|
        repo << row[attribute]
      end
      repo << (row[:owner] && row[:owner][:id])
      repo << start
      repos << repo

      star += row[:stargazers_count]
    end

    repo_ids = repos.map { |repo| repo[FETCH_ATTRIBUTES.index(:id)] }
    destroy_deleted_repos(user_id, repo_ids)

    Repository.import(
      FETCH_ATTRIBUTES + [:owner_id, :fetched_at],
      repos,
      on_duplicate_key_update: %i[name full_name description homepage stargazers_count language],
    )
    User.where(id: user_id).limit(1).update_all(stargazers_count: star, queued_at: Time.now)

    logger.info "Updated #{all_rows.size} repos for #{user_id}: #{Time.now - start}s"
  end

  private

  def all_repos(user_id)
    client = Github::LimitBalancer.instance.client
    client.auto_paginate = true
    client.repos(user_id)
  end

  def destroy_deleted_repos(user_id, repo_ids)
    Repository.where(owner_id: user_id).find_each do |repository|
      repository.destroy if repo_ids.exclude?(repository.id)
    end
  end

  def logger
    @logger ||= Logger.new('log/star_count_job.log')
  end
end
