class UsersController < ApplicationController
  PER_PAGE = 100

  def index
    @users = User.not_organization.starred_first.first(PER_PAGE)
  end

  def show
    @user = User.find_by!(login: params[:login])
  end
end
