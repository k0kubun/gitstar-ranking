class RepositoriesController < ApplicationController
  def index
    @repos = Repository.preload(:owner).order(stargazers_count: :desc).first(50)
    @repos_count = Repository.last.id
  end
end
