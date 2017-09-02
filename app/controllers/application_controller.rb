class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_action :set_raven_context

  private

  # devise authentication fail redirection
  def new_session_path(resource)
    root_path
  end

  def set_raven_context
    if current_user
      Raven.user_context(login: current_user.login)
    end
  end
end
