module Concerns
  module Rankable
    extend ActiveSupport::Concern

    included do |base|
      class_attribute :ranking_key
    end

    def rank
      Redis.current.zcount(ranking_key, "(#{stargazers_count}", '+inf') + 1
    rescue Redis::CannotConnectError
      super
    end
  end
end
