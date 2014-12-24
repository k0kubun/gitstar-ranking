class UsersController < ApplicationController
  def index
    @users = User.first(30)
    @users_count = Rails.cache.fetch('users_count', expires_in: 1.hour) do
      User.count
    end
  end
end
