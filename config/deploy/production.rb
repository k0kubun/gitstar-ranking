set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'

server ENV['DEPLOY_SSH_HOST'], user: ENV['DEPLOY_SSH_USER'], roles: %w{web app db}
