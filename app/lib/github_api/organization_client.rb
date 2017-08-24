require 'github_api/v4/client'

class GithubApi::OrganizationClient < GithubApi::V4::Client
  PAGE_SIZE = 100

  # @param [String] organization_login - "login" field of organization
  # @param [String] user_login - "login" field of user
  def organization_member?(organization_login, user_login)
    # TODO: This should find user edge for `user_login` directly
    logins = organizations_for(user_login)
    logins.include?(organization_login)
  end

  private

  def organizations_for(user_login)
    logins = []
    cursor = nil
    loop do
      edges = graphql(query: <<~QUERY).dig('data', 'user', 'organizations', 'edges')
        query {
          user(login: #{user_login.dump}) {
            organizations(first: #{PAGE_SIZE} #{("after: #{cursor.dump}" if cursor)}) {
              edges {
                node {
                  login
                }
                cursor
              }
            }
          }
        }
      QUERY
      logins += edges.map { |e| e.dig('node', 'login') }

      if edges.size < PAGE_SIZE
        break
      else
        cursor = edges.last.fetch('cursor')
      end
    end
    logins
  end
end
