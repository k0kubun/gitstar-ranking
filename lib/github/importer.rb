require 'octokit'

module Github
  class Importer
    FETCH_ATTRIBUTES = {
      user: %i[
        id
        login
        avatar_url
        type
        site_admin
      ],
    }

    def import
      access_tokens.each do |access_token|
        client = Octokit::Client.new(access_token: access_token)
        import_users(client)
      end
    end

    private

    def import_users(client)
      users = []

      client.all_users.each do |row|
        user = User.new
        user.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES[:user])
        users << user
      end

      User.import(users)
    rescue => e
      puts "#{e.class}: #{e}"
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end
  end
end
