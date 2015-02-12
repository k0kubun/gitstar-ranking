module PaginationHelper
  # Large offset causes slow query.
  # FIXME: maybe kaminari has this kind of option
  def limited_paginate(scope, options = {}, &block)
    paginator = Kaminari::Helpers::Paginator.new self, options.reverse_merge(
      current_page: scope.current_page,
      per_page:     scope.limit_value,
      total_count:  1000,
      total_pages:  pagination_limit,
      remote:       false,
    )
    paginator.to_s
  end

  def sunspot_paginate(results, options = {}, &block)
    paginator = Kaminari::Helpers::Paginator.new self, options.reverse_merge(
      current_page: results.current_page,
      per_page:     results.limit_value,
      total_count:  results.total_count,
      total_pages:  [results.total_pages, pagination_limit].min,
      remote:       false,
    )
    paginator.to_s
  end
end
