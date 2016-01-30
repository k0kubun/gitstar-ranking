set :stage, :production
set :branch, 'master'
set :rails_env, 'production'
set :migration_role, 'db'
set :sidekiq_role, 'sidekiq'

set :rbenv_ruby, File.read(File.expand_path('../../.ruby-version', __dir__)).strip

set :user, 'k0kubun'
role :app, 'cloudcore'
role :web, 'cloudcore'
role :db,  []
role :sidekiq, []

set :deploy_to, '/home/k0kubun/github-ranking'
