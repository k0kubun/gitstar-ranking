class Repository < ActiveRecord::Base
  class_attribute :ranking_key
  self.ranking_key = 'github-ranking:repository:world:all'

  paginates_per 100

  belongs_to :owner, class_name: 'User'

  scope :starred_first, -> { order(stargazers_count: :desc) }

  def to_param
    name
  end

  def rank
    Redis.current.zcount(ranking_key, "(#{stargazers_count}", '+inf') + 1
  rescue Redis::CannotConnectError
    super
  end
end
