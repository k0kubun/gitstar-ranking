require 'thor'
require 'github_importer/tasks/import_all'

module GithubImporter
  class CLI < Thor
    desc 'all', 'Import all for initialization'
    def import_all
      Tasks::ImportAll.new.run
    end
  end
end
