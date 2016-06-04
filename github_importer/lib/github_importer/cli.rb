require 'thor'

module GithubImporter
  class CLI < Thor
    desc 'all', 'Import all for initialization'
    def import_all
      puts 'Import all'
    end
  end
end
