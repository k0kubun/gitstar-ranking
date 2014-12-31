class SearchesController < ApplicationController
  include Concerns::Pageable

  DEFAULT_SEARCH_TYPE = 'Users'
  SEARCH_TYPES = %w[Users Organizations Repositories].freeze
  PER_PAGE = 20

  before_filter :validate_page_param, only: :show

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

  def fetch_count
    search =
      case search_type
      when 'Users'
        search_users
      when 'Organizations'
        search_organizations
      when 'Repositories'
        search_repositories
      end
    render text: search.results.total_count
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
      paginate page: params[:page], per_page: PER_PAGE
      order_by :stargazers_count, :desc
      with :user_flag, true
    end
  end

  def search_organizations
    Organization.search do
      fulltext params[:q]
      paginate page: params[:page], per_page: PER_PAGE
      order_by :stargazers_count, :desc
    end
  end

  def search_repositories
    Repository.search do
      fulltext params[:q]
      paginate page: params[:page], per_page: PER_PAGE
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
