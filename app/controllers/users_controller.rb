class UsersController < ApplicationController
  include Concerns::Pageable

  PER_PAGE = 50

  before_action :validate_page_param, only: :index

  def index
    @users = User.not_organization.starred_first.page(params[:page])
    RankBuilder.new(UserRank).realtime_preload(@users)
  end

  def show
    @user = User.find_by!(login: params[:login])
    @repositories = @user.repositories.page(params[:page]).per(PER_PAGE)
  end

  def update_org
    @organization = User.find_by!(login: params[:organization][:login])
    raise ActionController::BadRequest unless current_user.member_of?(@organization.login)

    if @organization.queued_recently?
      redirect_to user_path(@organization), alert: 'Your organization has been already updated recently. Please retry later.'
      return
    end

    ActiveRecord::Base.transaction do
      @organization.update(queued_at: Time.now)
      UpdateUserJob.perform_later(user_id: @organization.id, token_user_id: current_user.id)
    end

    redirect_to user_path(@organization), notice: 'Update request is successfully queued. Please wait a moment.'
  end

  def update_myself
    if current_user.queued_recently?
      redirect_to current_user, alert: 'You have already updated your stars recently. Please retry later.'
      return
    end

    ActiveRecord::Base.transaction do
      current_user.update(queued_at: Time.now)
      UpdateUserJob.perform_later(user_id: current_user.id, token_user_id: current_user.id)
    end

    redirect_to current_user, notice: 'Update request is successfully queued. Please wait a moment.'
  end
end
