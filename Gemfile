source 'https://rubygems.org'

git_source(:github) do |repo_name|
  repo_name = "#{repo_name}/#{repo_name}" unless repo_name.include?('/')
  "https://github.com/#{repo_name}.git"
end

gem 'rails', '< 5.2'

gem 'activerecord-import'
gem 'bootstrap-sass'
gem 'bootswatch-rails'
gem 'devise', github: 'heartcombo/devise'
gem 'faraday'
gem 'font-awesome-rails'
gem 'google-analytics-rails'
gem 'hamlit'
gem 'jquery-rails'
gem 'kaminari'
gem 'omniauth'
gem 'omniauth-github'
gem 'omniauth-rails_csrf_protection'
gem 'pg'
gem 'puma'
gem 'rack-user_agent'
gem 'ridgepoler-rails'
gem 'sassc-rails'
gem 'sentry-raven'
gem 'sprockets'
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
