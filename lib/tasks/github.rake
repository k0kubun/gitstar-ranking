namespace :github do
  desc 'Import github data'
  task import: :environment do
    importer = Github::Importer.new
    importer.import
  end
end
