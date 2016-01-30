class Organization < User
  self.ranking_key = 'github-ranking:organization:world:all'

  paginates_per 100
end
