class Repository < ActiveRecord::Base
  include Concerns::Rankable
  include Concerns::Starrable

  REPOSITORY_RANKING_KEY = 'githubranking-repository-ranking'

  self.minimum_rankable_star = 15

  paginates_per 100

  belongs_to :owner, class_name: 'User'

  def to_param
    name
  end

  def ranking_key
    REPOSITORY_RANKING_KEY
  end
end
