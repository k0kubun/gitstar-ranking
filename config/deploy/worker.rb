set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'

set :user, 'k0kubun'
role :app, []
role :web, []
role :db,  []
role :worker, %w[gmocloud]
set :bundle_roles, :app

set :deploy_to, '/home/k0kubun/github-ranking-worker'

set :linked_dirs, %w[
  log
]
