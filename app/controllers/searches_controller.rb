class SearchesController < ApplicationController
  DEFAULT_SEARCH_TYPE = 'Users'
  SEARCH_TYPES = %w[Users Organizations Repositories].freeze

  def show
    case search_type
    when 'Users'
      search = search_users
      @users = search.results
    when 'Organizations'
      search = search_organizations
      @orgs  = search.results
    when 'Repositories'
      search = search_repositories
      @repos = preload_owners(search.results)
    end
  end

  private

  # Original association preloader.
  # Preload can't be applied to sunspot results.
  def preload_owners(repos)
    user_ids   = repos.map(&:owner_id)
    user_by_id = User.where(id: user_ids).index_by(&:id)

    repos.each do |repo|
      owner = user_by_id[repo.owner_id]
      repo.association(:owner).target = owner
    end
  end

  def search_users
    User.search do
      fulltext params[:q]
      paginate page: 1, per_page: 10
      order_by :stargazers_count, :desc
      with :user_flag, true
    end
  end

  def search_organizations
    Organization.search do
      fulltext params[:q]
      paginate page: 1, per_page: 10
      order_by :stargazers_count, :desc
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
