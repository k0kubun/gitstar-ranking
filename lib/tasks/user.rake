namespace :user do
  desc 'Update user'
  task update: :environment do
    Github::UserUpdater.new.run
  end

  desc 'Fetch user'
  task fetch: :environment do
    Github::UserFetcher.new.run
  end
end
