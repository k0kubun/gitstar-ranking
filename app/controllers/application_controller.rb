class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  helper_method :organization_member?

  private

  # devise authentication fail redirection
  def new_session_path(resource)
    root_path
  end


  def organization_member?(org, user)
    Octokit.organization_member?(org, user)
  end
end
