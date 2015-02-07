class FailureSweeper
  BATCH_SIZE = 10000

  def run
    max_id    = User.last.id
    max_index = max_id / BATCH_SIZE

    0.upto(max_index).each do |index|
      min = index * BATCH_SIZE
      max = (index + 1) * BATCH_SIZE

      user_ids = User.joins('LEFT OUTER JOIN repositories ON users.id = repositories.owner_id').
        where('repositories.id IS NULL').where('users.id BETWEEN :min AND :max', min: min, max: max).
        pluck(:id)
      user_ids.each do |user_id|
        UserFetchJob.perform_later(user_id)
      end

      User.where(id: user_ids).update_all(queued_at: Time.now)
      logger.info("Sweep between #{min}..#{max} (#{user_ids.size})")
    end
  end

  private

  def logger
    @logger ||= Logger.new('log/fail_sweeper.log')
  end
end
