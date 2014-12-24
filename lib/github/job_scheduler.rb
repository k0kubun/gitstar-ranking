module Github
  class JobScheduler
    SCHEDULE_COUNT = 500

    def self.schedule
      user_ids = User.where(queued_at: nil).limit(SCHEDULE_COUNT).pluck(:id)
      user_ids.each do |user_id|
        UserUpdateJob.perform_later(user_id)
      end
      User.where(id: user_ids).update_all(queued_at: Time.now)
    end
  end
end
