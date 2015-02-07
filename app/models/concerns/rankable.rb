module Concerns
  module Rankable
    extend ActiveSupport::Concern

    included do |base|
      class_attribute :minimum_rankable_star
    end

    def rank
      if stargazers_count >= minimum_rankable_star
        super
      else
        0
      end
    end
  end
end
