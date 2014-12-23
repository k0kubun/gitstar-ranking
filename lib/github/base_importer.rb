require 'octokit'
require 'logger'

module Github
  class BaseImporter
    def import
      raise NotImplementedError
    end

    private

    def select_client
      Octokit::Client.new(access_token: select_token)
    end

    def select_token
      count = access_tokens.length
      index = Time.now.min % count
      logger.info "Selected #{index}th token"

      access_tokens[index]
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end

    def logger
      @logger ||= Logger.new('log/user_importer.log')
    end
  end
end
