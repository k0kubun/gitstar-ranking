class AccessTokensController < ApplicationController
  def index
    unless current_user.admin? # double check with devise
      raise ActionController::BadRequest
    end
    @access_tokens = AccessToken.preload(:user).page(params[:page])
  end
end
