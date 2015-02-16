class Organization < User
  ORGANIZATION_RANKING_KEY = 'githubranking-organization-ranking'

  paginates_per 100

  def ranking_key
    ORGANIZATION_RANKING_KEY
  end
end
