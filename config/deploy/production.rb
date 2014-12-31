set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'
set :sidekiq_role, :app

server ENV['DEPLOY_SSH_HOST'], user: ENV['DEPLOY_SSH_USER'], roles: %w[web app db]
