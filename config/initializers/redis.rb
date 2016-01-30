redis_url = 'redis://localhost:6379'

Sidekiq.redis = { url: redis_url, namespace: 'github-ranking-sidekiq' }
Rails.application.config.cache_store = :redis_store, "#{redis_url}/0/cache", { expires_in: 90.minutes }
