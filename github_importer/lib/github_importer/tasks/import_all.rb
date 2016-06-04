require 'github_importer/import_worker'
require 'github_importer/request_worker'
require 'github_importer/scheduler'

module GithubImporter
  module Tasks
    class ImportAll
      # 1. Return a queue which dequeues user_id. (logging progress)
      # 2. Return a queue which dequeues an API response.
      # 3. Dequeue a struct and stores data to MySQL and Redis.
      def run
        scheduler = Scheduler.new
        scheduler.start

        requester = RequestWorker.new(scheduler.queue)
        requester.start

        importer = ImportWorker.new(requester.queue)
        importer.start

        wait
      end

      private

      def wait
        # Eternal wait.
        Thread::Queue.new.deq
      end
    end
  end
end
