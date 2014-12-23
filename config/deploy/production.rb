set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'

server 'conoha', user: 'k0kubun', roles: %w{web app db}
