class UsersController < ApplicationController
  def index
    @users = User.first(30)
    @users_count = User.count
  end
end
