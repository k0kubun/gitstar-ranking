class UsersController < ApplicationController
  def index
    @users = User.where(type: 'User').order(stargazers_count: :desc).first(30)
    @users_count = User.last.id
  end

  def orgs
    @users = User.where(type: 'Organization').order(stargazers_count: :desc).first(50)
    @users_count = User.last.id
    render action: :index
  end
end
