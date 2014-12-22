class UsersController < ApplicationController
  def index
    @users = User.first(20)
  end
end
