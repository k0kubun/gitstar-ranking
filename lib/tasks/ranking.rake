desc 'Update user, organization and repository ranking'
task ranking: :environment do
  Github::RankingInitializer.new.run
end
