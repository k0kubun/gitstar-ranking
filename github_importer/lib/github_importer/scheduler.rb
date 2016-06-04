module GithubImporter
  class Scheduler
    QUEUE_LENGTH = 100

    # Thread::SizedQueue<Integer> - user_id
    attr_reader :queue

    def initialize
      @queue = Thread::SizedQueue.new(QUEUE_LENGTH)
    end

    def start
      puts 'start scheduling.'
    end
  end
end
