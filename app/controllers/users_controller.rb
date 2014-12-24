class UsersController < ApplicationController
  def index
    @users = User.first(30)
    @users_count = User.last.id

    UserUpdateJob.perform_later(1)
  end
end
