module Github
  class JobScheduler
    SET_LENGTH = 1000
    SET_COUNT  = 8

    def self.schedule
      user_ids = User.where(queued_at: nil).limit(SET_LENGTH * SET_COUNT).pluck(:id)
      user_ids.each_slice(SET_LENGTH) do |id_set|
        StarCountJob.perform_later(id_set)
      end
      User.where(id: user_ids).update_all(queued_at: Time.now)
    end
  end
end
