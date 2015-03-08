module Github
  class RankingInitializer
    include Iteratable

    def run
      initialize_users
      initialize_organizations
      initialize_repositories
    end

    private

    def initialize_users
      columns = [:id, :stargazers_count]
      iterate_all(User.not_organization, columns, 0, true) do |id, stargazers_count|
        redis.zadd(User.ranking_key, stargazers_count, id) if stargazers_count > 0
      end
      puts
    end

    def initialize_organizations
      columns = [:id, :stargazers_count]
      iterate_all(Organization.all, columns, 0, true) do |id, stargazers_count|
        redis.zadd(Organization.ranking_key, stargazers_count, id) if stargazers_count > 0
      end
      puts
    end

    def initialize_repositories
      columns = [:id, :stargazers_count]
      iterate_all(Repository.all, columns, 0, true) do |id, stargazers_count|
        redis.zadd(Repository.ranking_key, stargazers_count, id) if stargazers_count > 0
      end
      puts
    end

    def redis
      @redis ||= Redis.current
    end
  end
end
