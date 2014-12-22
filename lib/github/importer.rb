require 'octokit'

module Github
  class Importer
    def import
      access_tokens.each do |access_token|
        client = Octokit::Client.new(access_token: access_token)
        import_users(client)
      end
    end

    private

    def import_users(client)
      users = client.all_users
    rescue => e
      puts "#{e.class}: #{e}"
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end
  end
end
