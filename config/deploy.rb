set :application, 'githubranking'
set :repo_url, 'git@github.com:k0kubun/githubranking.com.git'
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

set :ssh_options, {
  keys: [File.expand_path('~/.ssh/id_rsa')],
  forward_agent: true,
  auth_methods: %w(publickey)
}

namespace :deploy do
  desc 'Restart application'
  task :restart do
    invoke 'unicorn:restart'
  end
end
after 'deploy:publishing', 'deploy:restart'
after 'deploy:updated', 'newrelic:notice_deployment'
