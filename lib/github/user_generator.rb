module Github
  class UserGenerator
    FETCH_ATTRIBUTES = %i[id login avatar_url type]

    def run(login)
      user = User.new(fetch_attributes_of(login))
      user.save!
      user
    end

    private

    def fetch_attributes_of(login)
      user = Octokit.user(login)

      user.each_with_object({}) do |(key, value), user_attributes|
        user_attributes[key] = value if FETCH_ATTRIBUTES.include?(key)
      end
    end
  end
end
