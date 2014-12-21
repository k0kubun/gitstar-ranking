require 'github/importer'

namespace :github do
  desc 'Import github data'
  task :import do
    importer = Github::Importer.new
    importer.import
  end
end
