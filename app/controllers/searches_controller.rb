class SearchesController < ApplicationController
  DEFAULT_SEARCH_TYPE = 'Users'
  SEARCH_TYPES = %w[Users Repositories].freeze

  def show
    case search_type
    when 'Users'
      search = search_users
      @users = search.results
    when 'Repositories'
      search = search_repositories
      @repos = search.results
    end
  end

  private

  def search_users
    User.search do
      fulltext params[:q]
      paginate page: 1, per_page: 10
      order_by :stargazers_count, :desc
      with :user_flag, true
    end
  end

  def search_repositories
    Repository.search do
      fulltext params[:q]
      paginate page: 1, per_page: 10
      order_by :stargazers_count, :desc
    end
  end

  def search_type
    @search_type ||=
      if search_types.include?(params[:type])
        params[:type]
      else
        DEFAULT_SEARCH_TYPE
      end
  end
  helper_method :search_type

  def search_types
    SEARCH_TYPES
  end
  helper_method :search_types
end
