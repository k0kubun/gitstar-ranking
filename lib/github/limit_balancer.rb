require 'octokit'
require 'singleton'

module Github
  class LimitBalancer
    include Singleton

    def client
      Octokit::Client.new(access_token: access_tokens.sample)
    end

    def token_count
      access_tokens.size
    end

    private

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end

    def logger
      @logger ||= Logger.new('log/api_limit.log')
    end
  end
end
