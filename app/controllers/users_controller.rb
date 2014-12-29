class UsersController < ApplicationController
  def index
    @users = User.not_organization.starred_first.first(30)
    @users_count = User.last.id
  end

  def show
    @user = User.find_by!(login: params[:login])
  end

  def orgs
    @users = Organization.starred_first.first(50)
    @users_count = User.last.id
    render action: :index
  end
end
