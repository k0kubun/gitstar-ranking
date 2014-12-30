class OrganizationsController < ApplicationController
  PER_PAGE = 100

  def index
    @organizations = Organization.starred_first.first(PER_PAGE)
  end
end
