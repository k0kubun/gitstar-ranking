class UsersController < ApplicationController
  include Concerns::Pageable

  GITHUB_HOST = 'https://github.com'
  PER_PAGE = 50

  before_action :validate_page_param, only: :index

  def index
    @users = User.not_organization.starred_first.page(params[:page])
    RankBuilder.new(UserRank).realtime_preload(@users)
  end

  def show
    @user = User.find_by!(login: params[:login])
    @repositories = @user.repositories.page(params[:page]).per(PER_PAGE)
  rescue ActiveRecord::RecordNotFound
    user_name = request.path.sub(/\A\//, '')
    if current_user && user_exists_on_github?(user_name)
      UpdateUserJob.perform_later(token_user_id: current_user.id, user_name: user_name)
      redirect_to root_path, notice: "Requested to create a page for #{user_name.dump}. Wait for a while and check it later."
    else
      raise
    end
  end

  def update_later
    user = User.find_by!(login: params[:login])

    if user.queued_recently?
      redirect_to user_path(user), alert: 'It has been already updated recently. Please retry later.'
      return
    end

    ActiveRecord::Base.transaction do
      user.update(queued_at: Time.now)
      UpdateUserJob.perform_later(user_id: user.id, token_user_id: current_user.id)
    end

    redirect_to user_path(user), notice: 'Update request is successfully queued. Please wait a moment.'
  end

  private

  def user_exists_on_github?(user_name)
    conn = Faraday.new(url: GITHUB_HOST)
    conn.get("/#{user_name}").status == 200
  end
end
