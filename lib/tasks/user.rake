namespace :user do
  desc 'Fetch user'
  task fetch: :environment do
    Github::UserFetcher.new.run
  end

  desc 'Initialize user'
  task create: :environment do
    Github::UserGenerator.new.run(ENV['LOGIN'])
  end
end
