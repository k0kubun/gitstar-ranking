desc 'Update user, organization and repository ranking'
task :ranking do
  Github::RankingInitializer.new.run
end
