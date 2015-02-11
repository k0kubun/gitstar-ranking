require 'thread'

module Github
  class UserFetcher
    include Loggable

    BATCH_SIZE = 1000
    REQUEST_CONCURRENCY   = 80
    IMPORT_BUFFERING_SIZE = 100

    def initialize
      @max_queued_at = User.queued_last.last.updated_at
      logger.info("[start] max_queued_at: #{@max_queued_at}")

      @request_queue = SizedQueue.new(BATCH_SIZE)
      @result_queue  = Queue.new
      @destroy_queue = Queue.new

      @threads = []
    end

    def run
      REQUEST_CONCURRENCY.times do
        @threads << start_request_worker
      end
      @threads << start_result_importer
      @threads << start_destroyer

      total_count = 0

      loop do
        ids = target_user_ids
        break if ids.blank?

        ids.each do |id|
          @request_queue.push(id)
        end

        total_count += ids.size
        logger.info(total_count)

        # to interrupt execution, `touch /tmp/user_fetcher`
        break if File.exists?('/tmp/user_fetcher')
      end

      @threads.each(&:kill)
    end

    private

    def start_request_worker
      client = AccessToken.fetch_client

      Thread.start(client, @request_queue, @result_queue, @destroy_queue) do |client, request, result, destroy|
        loop do
          user_id = request.pop

          begin
            user = client.user(user_id)
            result.push(user)
          rescue Octokit::NotFound
            destroy.push(user_id)
          end
        end
      end
    end

    def start_result_importer
      Thread.start(@result_queue) do |result|
        loop do
          results = []
          IMPORT_BUFFERING_SIZE.times do
            results << result.pop
          end

          User.import_updates(results)
        end
      end
    end

    def start_destroyer
      Thread.start(@destroy_queue) do |user_id|
        loop do
          User.where(user_id: user_id).delete_all
        end
      end
    end

    def target_user_ids
      users = User.queued_last.limit(BATCH_SIZE).pluck(:id, :queued_at)
      users.select! do |id, queued_at|
        queued_at < @max_queued_at
      end
      users.map(&:first)
    end
  end
end
