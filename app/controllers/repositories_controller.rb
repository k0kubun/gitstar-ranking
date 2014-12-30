class RepositoriesController < ApplicationController
  PER_PAGE = 100

  def index
    @repositories = Repository.preload(:owner).starred_first.first(PER_PAGE)
  end

  def show
    @repo = Repository.find_by!(full_name: "#{params[:user_login]}/#{params[:name]}")
  end
end
