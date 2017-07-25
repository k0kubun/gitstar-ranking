namespace :user do
  desc 'Initialize user'
  task create: :environment do
    Github::UserGenerator.new.run(ENV['LOGIN'])
  end
end
