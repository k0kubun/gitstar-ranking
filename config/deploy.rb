set :application, 'github-ranking'
set :repo_url, 'https://github.com/k0kubun/github-ranking.git'
set :log_level, :info

namespace :deploy do
  desc 'Restart application'
  task :restart do
    invoke 'unicorn:restart'
  end
end
after 'deploy:publishing', 'deploy:restart'

before 'deploy:starting', 'worker:stop'
after 'deploy:publishing', 'worker:start'
