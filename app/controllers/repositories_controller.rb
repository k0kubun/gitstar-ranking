class RepositoriesController < ApplicationController
  def index
    @repos = Repository.preload(:owner).order(stargazers_count: :desc).first(50)
    @repos_count = Repository.last.id
  end

  def show
    @repo = Repository.find_by!(full_name: "#{params[:user_login]}/#{params[:name]}")
  end
end
