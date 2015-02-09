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

  def self.perform_for(login)
    user = User.find_by!(login: login)
    self.new.perform(user.id)
  end

  def perform(user_id)
    start    = Time.now
    star     = 0

    begin
      all_rows = all_repos(user_id)
    rescue Octokit::NotFound => e
      User.where(id: user_id).delete_all
      Repository.where(owner_id: user_id).delete_all
      logger.info("Not found on GitHub. Destroyed user #{user_id}.")
      return
    end

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

    ActiveRecord::Base.transaction do
      repo_ids = repos.map { |repo| repo[FETCH_ATTRIBUTES.index(:id)] }
      destroy_deleted_repos(user_id, repo_ids)

      Repository.import(
        FETCH_ATTRIBUTES + [:owner_id, :fetched_at],
        repos,
        on_duplicate_key_update: %i[name full_name description homepage stargazers_count language],
      )
      User.where(id: user_id).limit(1).update_all(stargazers_count: star, updated_at: Time.now)
    end

    logger.info "Updated #{all_rows.size} repos for #{user_id}(#{User.find(user_id).login}): #{Time.now - start}s" if all_rows.size > 0
  rescue => e
    ExceptionNotifier.notify_exception(
      e,
      data: { message: "Detected exception for #{user_id}" },
    )
    raise e
  end

  private

  def all_repos(user_id)
    client = AccessToken.fetch_client
    client.auto_paginate = true
    result = client.repos(user_id)

    AccessToken.fetch_rate_limit(client.access_token)
    result
  end

  def destroy_deleted_repos(user_id, repo_ids)
    Repository.where(owner_id: user_id).find_each do |repository|
      repository.destroy if repo_ids.exclude?(repository.id)
    end
  end

  def logger
    @logger ||= Logger.new('log/user_fetch_job.log')
  end
end
