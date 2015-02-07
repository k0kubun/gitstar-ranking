class RankingUpdater
  BATCH_SIZE   = 1000

  def initialize(klass, scope)
    logger.info("== #{klass}.#{scope} ranking update ==")

    @klass    = klass
    @relation = klass.public_send(scope)
  end

  def run(logic_change_threshold:, min_star:)
    benchmark('TOTAL') do
      last_star, last_rank = benchmark("rank_up_top #{logic_change_threshold}") do
        rank_up_top(logic_change_threshold)
      end
      rank_up_between(last_star, min_star, last_rank)
    end
  end

  private

  def rank_up_top(limit)
    rank    = 0
    index   = 0
    current = nil
    values  = []

    relation = @relation.starred_first.select([:id, :stargazers_count])

    last_page = (limit - 1) / BATCH_SIZE + 1
    per_page  = [BATCH_SIZE, limit].min
    1.upto(last_page).each do |page|
      relation.page(page).per(per_page).each do |record|
        index += 1
        rank = index if record.stargazers_count != current
        current = record.stargazers_count

        values << [record.id, rank]
      end
    end

    @klass.import(
      [:id, :rank],
      values,
      on_duplicate_key_update: [:rank],
      validate: false,
    )

    [current, rank]
  end

  def rank_up_between(max_star, min_star, rank)
    max_star.downto(min_star).each do |star|
      benchmark("set_rank(#{star}, #{rank})") do
        set_rank(star, rank)
        rank = rank + @relation.where(stargazers_count: star).count
      end
    end
  end

  def set_rank(star, rank)
    @relation.where(stargazers_count: star).update_all(rank: rank)
  end

  def benchmark(name, &block)
    start  = Time.now
    result = block.call

    diff = Time.now - start
    logger.info("#{name} (#{Time.now - start}s)") if diff > 1.0

    result
  end

  def max_under(star)
    @relation.where('stargazers_count < ?', star).
      pluck('MAX(stargazers_count)').first
  end

  def logger
    @logger ||= Logger.new('log/ranking_updater.log')
  end
end
