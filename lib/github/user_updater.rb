require 'thread'

module Github
  class UserUpdater
    include Loggable
    include Interruptable

    API_REQUEST_CONCURRENCY = 80

    def initialize
      @max_queued_at = User.queued_last.last.updated_at
      logger.info("[start] max_queued_at: #{@max_queued_at}")
    end

    def run
      log_index = 0
      count = 0
      puts Time.now

      loop do
        ids = target_user_ids
        break if ids.blank?

        update_users(ids)
        count += ids.size

        log_index -= 1
        if log_index <= 0
          log_index = 100
          logger.info("#{count}")
        end

        accept_interrupt
      end

      logger.info('[finish]')
    end

    private

    def update_users(user_ids)
      results, absent_ids = parallel_api_request(user_ids)

      User.import_updates(results)
      User.where(id: absent_ids).delete_all
    end

    def parallel_api_request(user_ids)
      client  = AccessToken.fetch_client
      threads = []

      user_ids.each do |user_id|
        threads << Thread.new(user_id) do |id|
          begin
            client.user(id)
          rescue Octokit::NotFound
            id
          end
        end
      end
      results = threads.map(&:value)

      users      = []
      absent_ids = []
      results.each do |result|
        if result.is_a?(Sawyer::Resource)
          users << result
        else
          absent_ids << result
        end
      end

      [users, absent_ids]
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
