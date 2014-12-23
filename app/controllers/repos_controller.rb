class ReposController < ApplicationController
  def index
    @repos = Repo.first(20)
  end
end
