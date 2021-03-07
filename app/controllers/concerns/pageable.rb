module Concerns
  module Pageable
    extend ActiveSupport::Concern

    included do |base|
      helper_method :pagination_limit
    end

    private

    def validate_page_param
      if params[:page].present? && params[:page].to_i > pagination_limit
        raise ActionController::BadRequest
      end
    end

    def pagination_limit
      50
    end
  end
end
