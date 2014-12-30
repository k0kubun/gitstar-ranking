module ApplicationHelper
  # Large offset causes slow query
  def limited_paginate(scope, options = {}, &block)
    paginator = Kaminari::Helpers::Paginator.new self, options.reverse_merge(
      current_page: scope.current_page,
      per_page:     scope.limit_value,
      total_count:  1000,
      total_pages:  10,
      remote:       false,
    )
    paginator.to_s
  end
end
