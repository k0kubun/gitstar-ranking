class UsersController < ApplicationController
  include Concerns::Pageable

  PER_PAGE = 50

  before_filter :validate_page_param, only: :index

  def index
    @users = User.not_organization.starred_first.page(params[:page])
  end

  def show
    @user = User.find_by!(login: params[:login])
    @repositories = @user.repositories.page(params[:page]).per(PER_PAGE)
  end

  def update_myself
    if current_user.queued_recently?
      redirect_to current_user, alert: 'You have already updated your stars recently. Please retry later.'
      return
    end

    UserFetchJob.perform_later(current_user.id)
    current_user.update(queued_at: Time.now)

    redirect_to current_user, notice: 'Update request is successfully queued. Please wait a moment.'
  end
end
