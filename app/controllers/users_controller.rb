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
end
