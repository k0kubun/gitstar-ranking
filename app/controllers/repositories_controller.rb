class RepositoriesController < ApplicationController
  def index
    @repos = Repository.first(20)
    @repos_count = Rails.cache.fetch('repositories_count', expires_in: 1.hour) do
      Repository.count
    end
  end
end
