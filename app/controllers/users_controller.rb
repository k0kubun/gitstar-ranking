class UsersController < ApplicationController
  include Concerns::Pageable

  before_filter :validate_page_param, only: :index

  def index
    @users = User.not_organization.starred_first.page(params[:page])
  end

  def show
    @user = User.find_by!(login: params[:login])
  end
end
