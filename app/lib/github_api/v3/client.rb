require 'net/http'
require 'uri'

class GithubApi::V3::Client
  ENDPOINT = URI.parse('https://api.github.com')

  # @param [String] access_token
  def initialize(access_token)
    @access_token = access_token
  end

  # @param [String] organization_login - "login" field of organization
  # @param [String] user_login - "login" field of user
  # @return [Boolean]
  def organization_member?(organization_login, user_login)
    resp = get("/orgs/#{organization_login}/members/#{user_login}")
    resp.code == '204'
  end

  private

  def get(path)
    Net::HTTP.start(ENDPOINT.host, ENDPOINT.port, use_ssl: ENDPOINT.scheme == 'https') do |http|
      headers = {
        'Accept':        'application/vnd.github.v3+json',
        'Authorization': "bearer #{@access_token}",
        'Content-Type':  'application/json',
      }
      http.get(path, headers)
    end
  end
end
