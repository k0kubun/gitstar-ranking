class OrganizationsController < ApplicationController
  include Concerns::Pageable

  def index
    @organizations = Organization.starred_first.page(params[:page])
  end
end
