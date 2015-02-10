xml.instruct! :xml, version: '1.0'
xml.urlset xmlns: 'http://www.sitemaps.org/schemas/sitemap/0.9' do
  xml.url do
    xml.loc root_url
    xml.lastmod l(@user.updated_at, format: :lastmod)
    xml.changefreq 'hourly'
    xml.priority '1.0'
  end

  xml.url do
    xml.loc users_url
    xml.lastmod l(@user.updated_at, format: :lastmod)
    xml.changefreq 'hourly'
    xml.priority '0.9'
  end
  xml.url do
    xml.loc organizations_url
    xml.lastmod l(@organization.updated_at, format: :lastmod)
    xml.changefreq 'hourly'
    xml.priority '0.9'
  end
  xml.url do
    xml.loc repositories_url
    xml.lastmod l(@repository.updated_at, format: :lastmod)
    xml.changefreq 'hourly'
    xml.priority '0.9'
  end

  @users.first(5).each do |user|
    xml.url do
      xml.loc user_url(user)
      xml.lastmod l(user.updated_at, format: :lastmod)
      xml.changefreq 'daily'
      xml.priority '0.8'
    end
  end
  @organizations.first(5).each do |organization|
    xml.url do
      xml.loc user_url(organization)
      xml.lastmod l(organization.updated_at, format: :lastmod)
      xml.changefreq 'daily'
      xml.priority '0.8'
    end
  end
  @repositories.first(5).each do |repository|
    xml.url do
      xml.loc user_repository_url(repository.owner, repository.name)
      xml.lastmod l(repository.updated_at, format: :lastmod)
      xml.changefreq 'daily'
      xml.priority '0.8'
    end
  end

  @users.last(5).each do |user|
    xml.url do
      xml.loc user_url(user)
      xml.lastmod l(user.updated_at, format: :lastmod)
      xml.changefreq 'daily'
      xml.priority '0.7'
    end
  end
  @organizations.last(5).each do |organization|
    xml.url do
      xml.loc user_url(organization)
      xml.lastmod l(organization.updated_at, format: :lastmod)
      xml.changefreq 'daily'
      xml.priority '0.7'
    end
  end
  @repositories.last(5).each do |repository|
    xml.url do
      xml.loc user_repository_url(repository.owner, repository.name)
      xml.lastmod l(repository.updated_at, format: :lastmod)
      xml.changefreq 'daily'
      xml.priority '0.7'
    end
  end

  (2..10).each do |page|
    xml.url do
      xml.loc users_url(page: page)
      xml.lastmod l(@user.updated_at, format: :lastmod)
      xml.changefreq 'hourly'
      xml.priority "0.#{6 - (page / 2)}"
    end

    xml.url do
      xml.loc organizations_url(page: page)
      xml.lastmod l(@organization.updated_at, format: :lastmod)
      xml.changefreq 'hourly'
      xml.priority "0.#{6 - (page / 2)}"
    end

    xml.url do
      xml.loc repositories_url(page: page)
      xml.lastmod l(@repository.updated_at, format: :lastmod)
      xml.changefreq 'hourly'
      xml.priority "0.#{6 - (page / 2)}"
    end
  end
end
