# This can be only executed once without any other job execution
module GithubRanking::UserRankInitializer
  BATCH_SIZE = 5000

  class << self
    def run
      UserRank.delete_all

      log "Running init_records... (last_id = #{User.last.id})\n"
      init_records

      log "\nRunning init_ranks... (last_id = #{UserRank.last.id})\n"
      init_ranks
    end

    private

    def init_records
      User.find_in_batches(batch_size: BATCH_SIZE) do |users|
        records_by_stars = Hash.new { |h, k| h[k] = 0 }
        users.each do |user|
          if user.stargazers_count > 0 # skip stargazers_count = 0 for performance
            records_by_stars[user.stargazers_count] += 1
          end
        end
        add_records_by_stars(records_by_stars)

        log "#{users.last.id}\r"
      end
    end

    def add_records_by_stars(records_by_stars)
      total_records_by_starts = {}
      user_rank_by_stars = UserRank.where(stargazers_count: records_by_stars.keys).index_by(&:stargazers_count)

      records_by_stars.each do |stars, records|
        total_records_by_starts[stars] = records
        if (user_rank = user_rank_by_stars[stars])
          total_records_by_starts[stars] += user_rank.records_count
        end
      end

      UserRank.import(
        [:stargazers_count, :records_count],
        total_records_by_starts.to_a,
        on_duplicate_key_update: [:records_count],
      )
    end

    def init_ranks
      page = 1
      rank = 1
      while (user_ranks = UserRank.order(stargazers_count: :desc).page(page).per(BATCH_SIZE)).present?
        rank_by_stars = {}
        user_ranks.each do |user_rank|
          user_rank.rank = rank
          rank += user_rank.records_count
        end
        UserRank.import(
          [:stargazers_count, :records_count, :rank],
          user_ranks.map { |u| [u.stargazers_count, u.records_count, u.rank] },
          on_duplicate_key_update: [:rank],
        )
        page += 1

        log "#{user_ranks.last.id}\r"
      end
    end

    def log(message)
      unless Rails.env.test?
        print message
      end
    end
  end
end
