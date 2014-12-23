class UsersController < ApplicationController
  def index
    @users = User.first(30)
  end
end
