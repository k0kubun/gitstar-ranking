module Concerns
  module Rankable
    extend ActiveSupport::Concern

    included do |base|
      class_attribute :ranking_key
      class_attribute :minimum_rankable_star
    end

    def rank
      super
      # Redis.current.zcount(ranking_key, "(#{stargazers_count}", '+inf') + 1
    rescue Redis::CannotConnectError
      super
    end

    def update_ranking
      if stargazers_count > 0
        Redis.current.zadd(ranking_key, stargazers_count, id)
      else
        Redis.current.zrem(ranking_key, id)
      end
    end
  end
end
