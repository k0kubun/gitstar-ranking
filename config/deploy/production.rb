set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'
set :sidekiq_role, :sidekiq

set :user, 'k0kubun'
role :app, %w[gmocloud]
role :web, %w[gmocloud]
role :db,  []
role :sidekiq, %w[gmocloud]

set :deploy_to, '/home/k0kubun/github-ranking'
