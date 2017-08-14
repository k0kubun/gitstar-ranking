module Concerns
  module Rankable
    extend ActiveSupport::Concern

    included do |base|
      class_attribute :ranking_key
      class_attribute :minimum_rankable_star
    end

    def rank
      Redis.current.zcount(ranking_key, "(#{stargazers_count}", '+inf') + 1
    rescue Redis::CannotConnectError
      super
    end
  end
end
