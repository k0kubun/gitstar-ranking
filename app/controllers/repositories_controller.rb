class RepositoriesController < ApplicationController
  include Concerns::Pageable

  before_filter :validate_page_param, only: :index

  def index
    @repositories = Repository.preload(:owner).starred_first.page(params[:page])
  end

  def show
    @user = User.find_by!(login: params[:user_login])
    @repo = Repository.find_by!(full_name: "#{params[:user_login]}/#{params[:name]}")
  end
end
