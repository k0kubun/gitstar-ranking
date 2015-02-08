class AccessToken < ActiveRecord::Base
  CACHE_KEY   = 'access_token-rate_limit'
  TOKEN_LIMIT = 100

  paginates_per 50

  belongs_to :user

  def self.fetch_rate_limit(token)
    rate_limit = self.new(token: token).rate_limit_without_cache
    Rails.cache.write("#{CACHE_KEY}-#{token}", rate_limit, expires_in: 1.hour)
  end

  def self.fetch_client
    tokens = self.limit(TOKEN_LIMIT).pluck(:token)
    keys   = tokens.map { |token| "#{CACHE_KEY}-#{token}" }
    hits   = Rails.cache.read_multi(*keys)

    key   = hits.max_by { |k, v| v[:remaining] }.first
    token = key.gsub("#{CACHE_KEY}-", '')
    Octokit::Client.new(access_token: token)
  end

  def client
    Octokit::Client.new(access_token: token)
  end

  def rate_limit
    Rails.cache.fetch("#{CACHE_KEY}-#{token}", expires_in: 1.hour) do
      rate_limit_without_cache
    end
  end

  def rate_limit_without_cache
    if authorized?
      rate = client.rate_limit
      { remaining: rate.remaining, limit: rate.limit }
    else
      { remaining: 0, limit: 0 }
    end
  end

  private

  def authorized?
    client.rate_limit.present?
  rescue Octokit::Unauthorized
    false
  end
end
