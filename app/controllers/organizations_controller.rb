class OrganizationsController < ApplicationController
  include Concerns::Pageable

  before_action :validate_page_param, only: :index

  def index
    @organizations = Organization.starred_first.page(params[:page])
    RankBuilder.new(OrganizationRank).realtime_preload(@organizations)
    @stale_ids = @organizations.select { |u| u.not_queued_for_last?(1.days) }.map(&:id)
  end
end
