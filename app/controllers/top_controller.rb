class TopController < ApplicationController
  MAX_RECORDS = 10

  def index
    @users = User.not_organization.starred_first.first(MAX_RECORDS)
    @orgs  = Organization.starred_first.first(MAX_RECORDS)
    @repos = Repository.preload(:owner).starred_first.first(MAX_RECORDS)

    RankBuilder.new(UserRank).realtime_preload(@users)
    RankBuilder.new(OrganizationRank).realtime_preload(@orgs)
    RankBuilder.new(RepositoryRank).realtime_preload(@repos)
  end
end
