class SearchesController < ApplicationController
  def show
    search = User.search do
      fulltext params[:keyword]
      paginate page: 1, per_page: 10
    end
    @users = search.results
  end
end
