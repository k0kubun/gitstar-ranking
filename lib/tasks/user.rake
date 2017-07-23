namespace :user do
  desc 'Fetch user'
  task fetch: :environment do
    Github::UserFetcher.new.run
  end

  desc 'Initialize user'
  task create: :environment do
    Github::UserGenerator.new.run(ENV['LOGIN'])
  end

  namespace :rank do
    desc 'Initialize UserRank records'
    task init: :environment do
      GithubRanking::UserRankInitializer.run
    end
  end
end
