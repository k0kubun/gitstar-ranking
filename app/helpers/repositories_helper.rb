module RepositoriesHelper
  def normalize_url(url)
    if url =~ /^https?:\/\//
      url
    else
      "http://#{url}"
    end
  end
end
