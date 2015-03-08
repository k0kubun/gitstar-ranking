namespace :user do
  desc 'Update user'
  task update: :environment do
    Github::UserUpdater.new.run
  end

  desc 'Fetch user'
  task fetch: :environment do
    Github::UserFetcher.new.run
  end

  desc 'Update user location'
  task location: :environment do
    Github::LocationInitializer.new.run
  end
end
