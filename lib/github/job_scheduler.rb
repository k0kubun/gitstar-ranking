module Github
  class JobScheduler
    BATCH_SIZE = 6900

    def self.schedule
      user_ids = User.where(queued_at: nil).limit(BATCH_SIZE).pluck(:id)
      User.where(id: user_ids).update_all(queued_at: Time.now)

      filter_user_ids(user_ids).each do |user_id|
        UserFetchJob.perform_later(user_id)
      end
    end

    def self.filter_user_ids(user_ids)
      Repository.where(owner_id: user_ids).uniq.pluck(:owner_id)
    end
  end
end
