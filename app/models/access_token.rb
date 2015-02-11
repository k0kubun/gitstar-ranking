class AccessToken < ActiveRecord::Base
  TOKEN_LIST_KEY = 'access-token-list'
  TOKEN_LIMIT = 100

  paginates_per 50

  belongs_to :user

  def self.fetch_client
    Octokit::Client.new(access_token: fetch_token)
  end

  def client
    Octokit::Client.new(access_token: token)
  end

  private

  def self.fetch_token
    token = Redis.current.rpoplpush(TOKEN_LIST_KEY, TOKEN_LIST_KEY)
    return token if token.present?

    prepare_token_list
    Redis.current.rpoplpush(TOKEN_LIST_KEY, TOKEN_LIST_KEY)
  end

  def self.prepare_token_list
    tokens = self.limit(TOKEN_LIMIT).pluck(:token)
    tokens.each do |token|
      Redis.current.rpush(TOKEN_LIST_KEY, token)
    end
  end

  def authorized?
    client.rate_limit.present?
  rescue Octokit::Unauthorized
    false
  end
end
