class AccessToken < ActiveRecord::Base
  CACHE_KEY = 'access_token-rate_limit'

  paginates_per 50

  belongs_to :user

  def rate_limit
    Rails.cache.fetch("#{CACHE_KEY}-#{token}", expires_in: 1.hour) do
      rl = Octokit::Client.new(access_token: token).rate_limit
      { remaining: rl.remaining, limit: rl.limit }
    end
  end
end
