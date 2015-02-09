module Github
  class UserImporter
    API_REQUEST_INTERVAL = 1
    MAX_IMPORT_COUNT = 10
    FETCH_ATTRIBUTES = %i[
      id
      login
      avatar_url
      type
    ]

    def import
      logger.info 'User import task invoked'

      client = LimitBalancer.instance.client
      count  = User.last.id

      MAX_IMPORT_COUNT.times do
        import_users(client)
        sleep API_REQUEST_INTERVAL
      end

      logger.info "User last #{count} => #{Repository.last.id}"
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
