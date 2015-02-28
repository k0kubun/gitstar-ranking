class SearchesController < ApplicationController
  def show
    if @user = User.find_by(login: params[:q])
      redirect_to user_path(@user)
      return
    end

    flash[:warning] = "User '#{params[:q]}' is not found."
  end
end
