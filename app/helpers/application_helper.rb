module ApplicationHelper
  PAGE_TITLE  = 'GitHub Ranking'
  GITHUB_HOST = 'https://github.com'
  COLOR_CLASS = {
    notice: :success,
    alert: :danger,
  }

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

  def link_to_github(path, options = {}, &block)
    github_url = File.join(GITHUB_HOST, path)
    link_to github_url, options.merge(target: '_blank'), &block
  end

  def prepend_page_title(content)
    content_for :title, "#{content} - #{page_title}"
  end

  def append_page_title(content)
    content_for :title, "#{page_title} - #{content}"
  end

  def page_title
    PAGE_TITLE
  end

  def render_flash(type)
    if flash[type]
      content_tag(:div, class: "alert alert-dismissible alert-#{COLOR_CLASS[type]}") do
        flash[type]
      end
    end
  end
end
