class RepositoriesController < ApplicationController
  def index
    @repositories = Repository.preload(:owner).starred_first.page(params[:page])
  end

  def show
    @repo = Repository.find_by!(full_name: "#{params[:user_login]}/#{params[:name]}")
  end
end
