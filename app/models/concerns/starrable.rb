module Concerns
  module Starrable
    extend ActiveSupport::Concern

    included do |base|
      scope :starred_first, -> { order(stargazers_count: :desc) }
    end
  end
end
