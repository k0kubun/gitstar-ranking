class AccessTokensController < ApplicationController
  def index
    unless current_user.admin? # double check with devise
      raise ActionController::BadRequest
    end
    @access_tokens = AccessToken.preload(:user).page(params[:page])
  end

  def enabled
    unless current_user.admin? # double check with devise
      raise ActionController::BadRequest
    end
    @access_tokens = AccessToken.where(enabled: true).preload(:user).page(params[:page])
  end

  def enable
    current_user.access_token&.update!(enabled: true)
    redirect_to current_user
  end

  def disable
    current_user.access_token&.update!(enabled: false)
    redirect_to current_user
  end
end
