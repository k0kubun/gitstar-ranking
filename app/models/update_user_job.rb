class UpdateUserJob < ActiveRecord::Base
  # Kick UpdateUserWorker.java via MySQL queue
  # @param [Integer] user_id
  def self.perform_later(user_id)
    create(payload: user_id.to_s, timeout_at: Time.now)
  end
end
