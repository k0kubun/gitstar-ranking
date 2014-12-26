require 'octokit'
require 'singleton'

module Github
  class LimitBalancer
    include Singleton

    def client
      clients.max_by { |c| c.rate_limit.remaining }
    end

    def log_limit
      logger.info clients.map { |c| c.rate_limit.remaining }.join(',')
    end

    private

    def clients
      @clients ||= access_tokens.map { |t| Octokit::Client.new(access_token: t) }
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end

    def logger
      @logger ||= Logger.new('log/api_limit.log')
    end
  end
end
