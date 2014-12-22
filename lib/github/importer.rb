require 'octokit'

module Github
  class Importer
    BATCH_SIZE       = 100
    FETCH_ATTRIBUTES = {
      user: %i[
        id
        login
        avatar_url
        type
        site_admin
      ],
      repo: %i[
        id
        name
        full_name
        description
        homepage
        stargazers_count
        language
      ],
    }

    def import
      token     = access_tokens.pop
      client    = Octokit::Client.new(access_token: token)
      remaining = client.rate_limit.remaining

      ActiveRecord::Base.transaction do
        users = import_users(client)
        import_repos(client, users)
      end

      rl = client.rate_limit
      puts "API Limit Remaining: #{rl.remaining}/#{rl.limit} (used #{remaining - rl.remaining})"
    end

    private

    def import_users(client)
      last_id = User.pluck('MAX(id)').first
      users   = []

      client.auto_paginate = false
      client.all_users(since: last_id).each do |row|
        user = User.new
        user.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES[:user])
        users << user
      end

      User.import(users)
      users
    rescue => e
      puts "#{e.class}: #{e}"
      users
    end

    def import_repos(client, users)
      client.auto_paginate = true

      users.each do |user|
        rows = client.list_repos(user.id)
        rows.each_slice(BATCH_SIZE) do |row_group|
          repos = []

          row_group.each do |row|
            repo = Repo.new
            repo.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES[:repo])
            repo.owner_id   = row[:owner] && row[:owner][:id]
            repos << repo
          end

          Repo.import(repos)
        end
      end
    end

    def access_tokens
      @access_tokens ||= Rails.application.secrets[:github_access_tokens]
    end
  end
end
