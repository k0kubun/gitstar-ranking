source 'https://rubygems.org'

git_source(:github) do |repo_name|
  repo_name = "#{repo_name}/#{repo_name}" unless repo_name.include?('/')
  "https://github.com/#{repo_name}.git"
end

gem 'rails', '< 5.2'

gem 'activerecord-import'
gem 'bootstrap-sass'
gem 'bootswatch-rails'
gem 'coffee-rails'
gem 'devise'
gem 'dotenv-rails'
gem 'exception_notification'
gem 'faraday'
gem 'font-awesome-rails'
gem 'google-analytics-rails'
gem 'graphiql-rails', '1.4.8' # https://github.com/rmosolgo/graphiql-rails/issues/44
gem 'graphql'
gem 'graphql-batch'
gem 'hamlit'
gem 'jquery-rails'
gem 'kaminari'
gem 'mysql2'
gem 'newrelic_rpm'
gem 'omniauth'
gem 'omniauth-github', github: 'k0kubun/omniauth-github', ref: '7d7f67846c0a40c32fc36e09e0f8021755b979b8'
gem 'puma'
gem 'rack-user_agent'
gem 'ridgepoler-rails'
gem 'sassc-rails'
gem 'sentry-raven'
gem 'sprockets', '< 4'
gem 'uglifier'
gem 'webpacker'

group :development do
  gem 'foreman'
  gem 'silencer'
  gem 'spring'
  gem 'spring-commands-rspec'
  gem 'listen'
end

group :development, :test do
  gem 'factory_girl'
  gem 'pry-byebug'
  gem 'pry-doc'
  gem 'pry-rails'
  gem 'rspec-rails'
  gem 'timecop'
end
