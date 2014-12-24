module Github
  class JobScheduler
    SCHEDULE_COUNT = 100

    def self.schedule
      user_ids = User.where(queued_at: nil).limit(SCHEDULE_COUNT).pluck(:id)
      user_ids.each do |user_id|
        UserUpdateJob.perform_later(user_id)
      end
    end
  end
end
