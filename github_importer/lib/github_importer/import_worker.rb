module GithubImporter
  class ImportWorker
    def initialize(response_queue)
      @response_queue = response_queue
    end

    def start
      puts 'Start importing.'
    end
  end
end
