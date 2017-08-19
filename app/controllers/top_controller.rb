class TopController < ApplicationController
  MAX_RECORDS = 10

  def index
    @users = User.not_organization.starred_first.first(MAX_RECORDS)
    @orgs  = Organization.starred_first.first(MAX_RECORDS)
    @repos = Repository.preload(:owner).starred_first.first(MAX_RECORDS)

    RankBuilder.new(UserRank).preload(@users)
    RankBuilder.new(OrganizationRank).preload(@orgs)
    RankBuilder.new(RepositoryRank).preload(@repos)
  end
end
