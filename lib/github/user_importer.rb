require 'octokit'
require 'logger'

module Github
  class UserImporter
    API_REQUEST_INTERVAL = 0.05
    MAX_IMPORT_COUNT = 50
    FETCH_ATTRIBUTES = %i[
      id
      login
      avatar_url
      type
      site_admin
    ]

    def import
      logger.info 'User import task invoked'

      token     = select_token
      client    = Octokit::Client.new(access_token: token)
      remaining = client.rate_limit.remaining
      count     = User.count

      MAX_IMPORT_COUNT.times do
        import_users(client)

        rl = client.rate_limit
        logger.info "API Limit Remaining: #{rl.remaining}/#{rl.limit} (used #{remaining - rl.remaining})"

        remaining = rl.remaining
        sleep API_REQUEST_INTERVAL
      end

      logger.info "User count #{count} => #{User.count}"
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

    def select_token
      count = access_tokens.length
      index = Time.now.min % count
      logger.info "Selected #{index}th token"

      access_tokens[index]
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end

    def logger
      @logger ||= Logger.new('log/user_importer.log')
    end
  end
end
