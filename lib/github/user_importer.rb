require 'octokit'
require 'logger'

module Github
  class UserImporter
    API_REQUEST_INTERVAL = 3
    MAX_IMPORT_COUNT = 10
    FETCH_ATTRIBUTES = %i[
      id
      login
      avatar_url
      type
      site_admin
    ]

    def import
      logger.info 'User import task invoked'

      token     = access_tokens.first
      client    = Octokit::Client.new(access_token: token)
      remaining = client.rate_limit.remaining

      MAX_IMPORT_COUNT.times do
        import_users(client)

        rl = client.rate_limit
        logger.info "API Limit Remaining: #{rl.remaining}/#{rl.limit} (used #{remaining - rl.remaining})"

        remaining = rl.remaining
        sleep API_REQUEST_INTERVAL
      end
    rescue => e
      logger.error "#{e.class}: #{e}"
    end

    private

    def import_users(client)
      last_id = User.pluck('MAX(id)').first
      users   = []

      client.auto_paginate = false
      client.all_users(since: last_id).each do |row|
        user = User.new
        user.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES)
        users << user
      end

      User.import(users)
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end

    def logger
      @logger ||= Logger.new('log/user_importer.log')
    end
  end
end
