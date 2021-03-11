source 'https://rubygems.org'

git_source(:github) do |repo_name|
  repo_name = "#{repo_name}/#{repo_name}" unless repo_name.include?('/')
  "https://github.com/#{repo_name}.git"
end

gem 'rails', '< 5.2'

gem 'activerecord-import'
gem 'bootstrap-sass'
gem 'bootswatch-rails'
gem 'devise'
gem 'faraday'
gem 'font-awesome-rails'
gem 'google-analytics-rails'
gem 'hamlit'
gem 'jquery-rails'
gem 'kaminari'
gem 'omniauth'
gem 'omniauth-github', github: 'k0kubun/omniauth-github', ref: '7d7f67846c0a40c32fc36e09e0f8021755b979b8'
gem 'pg'
gem 'puma'
gem 'rack-user_agent'
gem 'ridgepoler-rails'
gem 'sassc-rails'
gem 'sentry-raven'
gem 'sprockets', '< 4' # needs app/assets/config/manifest.js
gem 'uglifier'

group :development do
  gem 'foreman'
  gem 'spring'
  gem 'spring-commands-rspec'
  gem 'listen'
end

group :development, :test do
  gem 'factory_bot'
  gem 'pry-rails'
  gem 'rspec-rails'
end
