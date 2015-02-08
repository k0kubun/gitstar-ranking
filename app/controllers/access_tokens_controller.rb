class AccessTokensController < ApplicationController
  def index
    @access_tokens = AccessToken.preload(:user).page(params[:page])
  end
end
