namespace :user do
  desc 'Update user'
  task update: :environment do
    Github::UserUpdater.new.run
  end

  desc 'Fetch user'
  task fetch: :environment do
    Github::UserFetcher.new.run
  end

  desc 'Initialize redis ranking'
  task redis: :environment do
    Github::RankingInitializer.new.run
  end
end
