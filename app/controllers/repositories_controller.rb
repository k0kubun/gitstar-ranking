class RepositoriesController < ApplicationController
  def index
    @repos = Repository.first(20)
  end
end
