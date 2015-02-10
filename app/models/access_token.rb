class AccessToken < ActiveRecord::Base
  CACHE_KEY   = 'access_token-rate_limit'
  TOKEN_LIMIT = 100

  paginates_per 50

  belongs_to :user

  def self.fetch_rate_limit(token)
    Rails.cache.delete("#{CACHE_KEY}-#{token}")
    self.new(token: token).rate_limit
  end

  def self.fetch_client
    token = self.limit(TOKEN_LIMIT).pluck(:token).sample
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
