module Github
  class LocationInitializer
    include Loggable
    include Iteratable

    CONCURRENCY = 4

    def run
      worker_pool = Worker.new CONCURRENCY, lambda { |user_id, worker_index|
        update_location(user_id)
      }

      iterate_all(User.all, [:id], 0, false) do |id|
        worker_pool.enq(id)
      end

      worker_pool.stop
    end

    private

    def update_location(id)
      client = AccessToken.fetch_client
      location = client.user(id)[:location]
      User.where(id: id).update_all(location: location)
      print "#{id}\r"
    rescue => e
      logger.error "#{e.class} (#{user_id}): #{e}"
    end
  end
end
