host = 'localhost'
port = 6379
redis_url = "redis://#{host}:#{port}"

Redis.current = Redis.new(host: host, port: port)
Rails.application.config.cache_store = :redis_store, "#{redis_url}/0/cache", { expires_in: 90.minutes }
