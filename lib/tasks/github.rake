namespace :github do
  desc 'Import github users'
  task import_users: :environment do
    importer = Github::UserImporter.new
    importer.import
  end
end
