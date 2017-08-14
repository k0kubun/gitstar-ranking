redis_url = 'redis://localhost:6379'
Rails.application.config.cache_store = :redis_store, "#{redis_url}/0/cache", { expires_in: 90.minutes }
