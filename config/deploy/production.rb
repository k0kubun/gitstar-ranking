set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'

set :user, 'k0kubun'
role :app, %w[gmocloud]
role :web, %w[gmocloud]
role :db,  []
role :worker, []

set :deploy_to, '/home/k0kubun/github-ranking'

set :linked_files, %w[
  .env
  config/database.yml
  config/newrelic.yml
  config/secrets.yml
]

set :linked_dirs, %w[
  log
  tmp/pids
  tmp/cache
  tmp/sockets
  vendor/bundle
  public/system
  public/assets
  public/packs
]
