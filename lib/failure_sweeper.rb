class FailureSweeper
  BATCH_SIZE  = 1000
  INTERVAL    = 100
  IMPORT_SIZE = 1000

  def run
    batch_size = BATCH_SIZE
    max_id     = User.last.id
    max_index  = max_id / batch_size
    scheduled  = 0
    total      = 0

    2.upto(max_index).each do |index|
      min = index * batch_size
      max = (index + 1) * batch_size

      user_ids = User.joins('LEFT OUTER JOIN repositories ON users.id = repositories.owner_id').
        where('repositories.id IS NULL').where('users.id BETWEEN :min AND :max', min: min, max: max).
        where('queued_at < ?', Time.local(2015, 2, 8, 13, 10)).
        pluck(:id)
      user_ids.each do |user_id|
        UserFetchJob.perform_later(user_id)
      end

      User.where(id: user_ids).update_all(queued_at: Time.now)
      logger.info("Sweep between #{min}..#{max} (#{user_ids.size})")

      scheduled += user_ids.size
      total     += user_ids.size
      if scheduled > INTERVAL
        scheduled = 0
        logger.info('sleep 5 minutes')
        sleep(5 * 60)
      end
      if total > IMPORT_SIZE
        logger.info('finish')
        return
      end
    end
  end

  private

  def logger
    @logger ||= Logger.new('log/failure_sweeper.log')
  end
end
