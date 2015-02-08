class AccessToken < ActiveRecord::Base
  CACHE_KEY = 'access_token-rate_limit'

  paginates_per 50

  belongs_to :user

  def client
    Octokit::Client.new(access_token: token)
  end

  def rate_limit
    Rails.cache.fetch("#{CACHE_KEY}-#{token}", expires_in: 1.hour) do
      rate = client.rate_limit
      { remaining: rate.remaining, limit: rate.limit }
    end
  end
end
