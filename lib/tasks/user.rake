namespace :user do
  desc 'Update user'
  task update: :environment do
    Github::UserUpdater.new.run
  end
end
