module Concerns
  module Pageable
    extend ActiveSupport::Concern

    included do |base|
      before_filter :validate_page_param, only: :index
    end

    private

    def validate_page_param
      if params[:page].present? && params[:page].to_i > 10
        raise ActionController::BadRequest
      end
    end
  end
end
