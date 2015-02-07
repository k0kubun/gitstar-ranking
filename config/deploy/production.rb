set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'
set :deploy_to, '/home/k0kubun/githubranks'

set :user, 'k0kubun'
role :app, 'conoha'
role :web, 'conoha'
role :db,  'excloud'
