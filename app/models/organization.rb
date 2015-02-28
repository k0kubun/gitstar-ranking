class Organization < User
  self.ranking_key = 'githubranking:organization:world:all'

  paginates_per 100
end
