require 'thread'

module Github
  class UserUpdater
    include Loggable
    include Interruptable

    API_REQUEST_CONCURRENCY = 30

    def initialize
      @max_queued_at = User.queued_last.last.updated_at
      logger.info("[start] max_queued_at: #{@max_queued_at}")
    end

    def run
      count = 0
      puts Time.now

      loop do
        ids = target_user_ids
        break if ids.blank?

        update_users(ids)
        count += ids.size
        print "#{count}\r"

        accept_interrupt
      end

      print "\nfinish\n"
    end

    private

    def update_users(user_ids)
      results = parallel_api_request(user_ids)
      User.import_updates(results)
    end

    def parallel_api_request(user_ids)
      client  = AccessToken.fetch_client
      threads = []

      user_ids.each do |user_id|
        threads << Thread.new(user_id) do |id|
          client.user(id)
        end
      end
      threads.map(&:value)
    end

    def target_user_ids
      users = User.queued_last.limit(API_REQUEST_CONCURRENCY).pluck(:id, :queued_at)
      users.select! do |id, queued_at|
        queued_at < @max_queued_at
      end
      users.map(&:first)
    end
  end
end
