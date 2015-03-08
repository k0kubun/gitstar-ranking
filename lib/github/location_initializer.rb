module Github
  class LocationInitializer
    include Loggable
    include Iteratable

    CONCURRENCY = 4

    def run
      iterate_all(User.all, [:id], 0, true) do |id|
        update_location(id)
      end
    end

    private

    def update_location(id)
      client = AccessToken.fetch_client
      location = client.user(id)[:location]
      User.where(id: id).update_all(location: location)
    rescue => e
      logger.error "#{e.class} (#{user_id}): #{e}"
    end
  end
end
