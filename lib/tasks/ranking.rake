desc 'Update user, organization and repository ranking'
task :ranking do
  %w[user organization repository].each do |target|
    Rake::Task["ranking:#{target}"].invoke
  end
end

namespace :ranking do
  desc 'Update user ranking'
  task user: :environment do
    job = RankingUpdater.new(User, :not_organization)
    job.run(logic_change_threshold: 10000, min_star: User.minimum_rankable_star)
  end

  desc 'Update organization ranking'
  task organization: :environment do
    # NOTE: this should not be Organization because STI table
    # requires :type column to import.
    job = RankingUpdater.new(User, :organization)
    job.run(logic_change_threshold: 5000, min_star: Organization.minimum_rankable_star)
  end

  desc 'Update repository ranking'
  task repository: :environment do
    job = RankingUpdater.new(Repository, :all)
    job.run(logic_change_threshold: 20000, min_star: Repository.minimum_rankable_star)
  end
end
