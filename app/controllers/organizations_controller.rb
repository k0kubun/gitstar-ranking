class OrganizationsController < ApplicationController
  include Concerns::Pageable

  before_filter :validate_page_param, only: :index

  def index
    @organizations = Organization.starred_first.page(params[:page])
    RankBuilder.new(OrganizationRank).realtime_preload(@organizations)
  end
end
