class UserUpdateJob < ActiveJob::Base
  queue_as :default

  def perform(user_id)
    logger.info("update: #{user_id}")
  end

  private

  def logger
    @logger ||= Logger.new('log/user_update_job.log')
  end
end
