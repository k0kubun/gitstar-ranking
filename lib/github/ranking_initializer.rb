module Github
  class RankingInitializer
    BATCH_SIZE = 5000

    def run
      initialize_users
      initialize_organizations
      initialize_repositories
    end

    private

    def iterate_all(relation, columns, last_id = 0, debug = false, &block)
      assert_first_column(columns, :id)

      loop do
        records = relation.dup.where('id > ?', last_id).
          limit(BATCH_SIZE).order(id: :asc).pluck(*columns)
        break if records.blank?

        records.each do |*args|
          block.call(*args)
        end
        last_id =
          if columns.size > 1
            records.last.first
          else
            records.last
          end
        print "#{last_id}\r" if debug
      end
    end

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

    def assert_first_column(columns, column)
      if columns.first != column
        raise ArgumentError, "First column must be #{column}"
      end
    end

    def redis
      @redis ||= Redis.current
    end
  end
end
