class RepositoriesController < ApplicationController
  include Concerns::Pageable

  GITHUB_HOST = 'https://github.com'

  before_filter :validate_page_param, only: :index

  def index
    @repositories = Repository.preload(:owner).starred_first.page(params[:page])
    RankBuilder.new(RepositoryRank).realtime_preload(@repositories)
  end

  def show
    @user = User.find_by!(login: params[:user_login])
    @repo = Repository.find_by!(full_name: "#{params[:user_login]}/#{params[:name]}")
  rescue ActiveRecord::RecordNotFound => e
    location = check_github_redirection
    raise e if location.blank?

    redirect_to location
  end

  private

  def check_github_redirection
    conn = Faraday.new(url: GITHUB_HOST)
    resp = conn.get(request.path)
    return if resp.status != 301

    location = resp.env.response_headers[:location]
    return if location == File.join(GITHUB_HOST, request.path)

    location.gsub(/^#{GITHUB_HOST}/, '')
  end
end
