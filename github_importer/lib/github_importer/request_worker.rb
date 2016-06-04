module GithubImporter
  class RequestWorker
    QUEUE_LENGTH = 1000

    # Thread::SizedQueue<Hash> - response
    attr_reader :queue

    def initialize(user_id_queue)
      @queue = Thread::SizedQueue.new(QUEUE_LENGTH)
      @user_id_queue = user_id_queue
    end

    def start
      puts 'start requesting.'
    end
  end
end
