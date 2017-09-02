module ApplicationHelper
  include PaginationHelper

  PAGE_TITLE  = 'Gitstar Ranking'
  GITHUB_HOST = 'https://github.com'
  COLOR_CLASS = {
    notice: :success,
    alert: :danger,
    warning: :warning,
  }

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
