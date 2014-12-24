class RepositoriesController < ApplicationController
  def index
    @repos = Repository.first(20)
    @repos_count = Repository.last.id
  end
end
