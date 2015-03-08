set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'
set :sidekiq_role, 'app'

set :user, 'k0kubun'
role :app, 'gmo'
role :web, 'gmo'
role :db,  'excloud'

set :deploy_to, '/home/k0kubun/githubranking'
