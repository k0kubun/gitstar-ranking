namespace :github do
  desc 'Import github users'
  task import_users: :environment do
    importer = Github::UserImporter.new
    importer.import
  end

  desc 'Import github repos'
  task import_repos: :environment do
    importer = Github::RepositoryImporter.new
    importer.import
  end
end
