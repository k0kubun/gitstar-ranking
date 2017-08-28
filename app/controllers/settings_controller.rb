class SettingsController < ApplicationController
  def show
    unless user_signed_in?
      redirect_to root_path
      return
    end

    if current_user.access_token.nil?
      # TODO: force logout?
    elsif current_user.access_token.enabled.nil?
      # Require to agree token usage first if not responded yet
      render :token_agreement
    end
  end
end
