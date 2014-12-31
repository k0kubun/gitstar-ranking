class UpdateRepoJob < ActiveJob::Base
  queue_as :default

  def perform(user_id)
    logger.log("Test: " + user_id.to_s)
  end
end
