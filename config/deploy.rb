set :application, 'github-ranking'
set :repo_url, 'git@github.com:k0kubun/github-ranking.git'
set :log_level, :info

set :linked_files, %w[
  .env
  config/database.yml
  config/secrets.yml
  config/initializers/redis.rb
]

set :linked_dirs, %w[
  bin
  log
  solr
  tmp/pids
  tmp/cache
  tmp/sockets
  vendor/bundle
  public/system
  public/assets
]

namespace :deploy do
  desc 'Restart application'
  task :restart do
    invoke 'unicorn:restart'
  end
end
after 'deploy:publishing', 'deploy:restart'

# NOTE: pending due to deployment failure
# require 'new_relic/recipes'
# after 'deploy:updated', 'newrelic:notice_deployment'
