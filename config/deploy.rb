set :application, 'github_ranks'
set :repo_url, 'git@github.com:k0kubun/github_ranks.git'
set :deploy_to, ENV['DEPLOY_TO']
set :log_level, :info

set :linked_files, %w[
  .env
  config/secrets.yml
]

set :linked_dirs, %w[
  bin
  log
  light_blue
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
