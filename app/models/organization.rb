class Organization < User
  self.ranking_key = 'github-ranking:organization:world:all'

  paginates_per 100

  def rank
    @rank ||= RankBuilder.new(OrganizationRank).build(self)
  end
end
