class Repository < ApplicationRecord
  attr_writer :rank

  paginates_per 100

  belongs_to :owner, class_name: 'User'

  scope :starred_first, -> { order(stargazers_count: :desc) }

  def to_param
    name
  end

  def rank
    @rank ||= RankBuilder.new(RepositoryRank).build(self)
  end
end
