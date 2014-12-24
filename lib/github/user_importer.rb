module Github
  class UserImporter
    API_REQUEST_INTERVAL = 0.02
    MAX_IMPORT_COUNT = 30
    FETCH_ATTRIBUTES = %i[
      id
      login
      avatar_url
      type
      site_admin
    ]

    def import
      logger.info 'User import task invoked'

      client    = LimitBalancer.instance.client
      remaining = client.rate_limit.remaining
      count     = User.last.id

      MAX_IMPORT_COUNT.times do
        import_users(client)

        rl = client.rate_limit
        logger.info "API Limit Remaining: #{rl.remaining}/#{rl.limit} (used #{remaining - rl.remaining})"

        remaining = rl.remaining
        sleep API_REQUEST_INTERVAL
      end

      logger.info "User count #{count} => #{User.last.id}"
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

    def logger
      @logger ||= Logger.new('log/user_importer.log')
    end
  end
end
