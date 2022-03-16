class SitemapsController < ApplicationController
  def show
    @users         = User.not_organization.starred_first.first(10)
    @organizations = Organization.starred_first.first(10)
    @repositories  = Repository.preload(:owner).starred_first.first(10)

    @user         = @users.first
    @organization = @organizations.first
    @repository   = @repositories.first

    respond_to { |format| format.xml }
  end
end
