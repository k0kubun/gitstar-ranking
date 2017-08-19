class Organization < User
  paginates_per 100

  def rank
    @rank ||= RankBuilder.new(OrganizationRank).build(self)
  end
end
