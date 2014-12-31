module Concerns
  module Pageable
    extend ActiveSupport::Concern

    private

    def validate_page_param
      if params[:page].present? && params[:page].to_i > 10
        raise ActionController::BadRequest
      end
    end
  end
end
