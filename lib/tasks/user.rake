namespace :user do
  desc 'Update user'
  task :update do
    Github::UserUpdater.new.run
  end
end
