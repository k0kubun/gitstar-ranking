class ReposController < ApplicationController
  def index
    @repos = Repo.order(stargazers_count: :desc).first(20)
  end
end
