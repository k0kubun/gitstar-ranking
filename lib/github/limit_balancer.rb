require 'octokit'
require 'singleton'

module Github
  class LimitBalancer
    include Singleton

    MAX_TRIAL        = 5
    MINIMUM_LIMIT    = 10
    CACHE_KEY_PREFIX = "github-rl-"

    def client
      Octokit::Client.new(access_token: fetch_token)
    end

    def token_count
      access_tokens.size
    end

    def cache_key(token)
      token.prepend(CACHE_KEY_PREFIX)
    end

    private

    def fetch_token
      MAX_TRIAL.times do
        token = access_tokens.sample
        return token if limit_for(token) > MINIMUM_LIMIT
      end

      access_tokens.each do |token|
        return token if limit_for(token) > MINIMUM_LIMIT
      end

      raise 'There is no remaining token any more'
    end

    def limit_for(token)
      Rails.cache.fetch(cache_key(token)) do
        Octokit::Client.new(access_token: token).rate_limit.remaining
      end
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end

    def logger
      @logger ||= Logger.new('log/api_limit.log')
    end
  end
end
