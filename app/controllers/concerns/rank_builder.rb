class RankBuilder
  REALTIME_THRESHOLD = 1000

  # @param [ActiveRecord::Base] rank_class - with stargazers_count, rank columns
  def initialize(rank_class)
    @rank_class = rank_class
  end

  # @param [ActiveRecord::Base] record
  def build(record)
    rank_record = @rank_class.where('stargazers_count <= ?', record.stargazers_count).order(stargazers_count: :desc).first
    result = guess(record, rank_record)
    return result if REALTIME_THRESHOLD < result

    calculate(record)
  end

  # Take records with `attr_writer :rank`, calculate realtime ranking on demand and set it.
  # @param [ActiveRecord::Relation,Array<ActiveRecord::Base>] records
  def realtime_preload(records)
    return if records.to_a.size == 0

    first = records.first
    firsts, records = records.partition { |r| r.stargazers_count == first.stargazers_count }

    prev_rank  = first.rank = calculate(first)
    prev_stars = first.stargazers_count
    prev_count = resolve_sti(first).where(stargazers_count: first.stargazers_count).count

    firsts.each { |f| f.rank = first.rank }
    records.each do |record|
      if record.stargazers_count < prev_stars
        prev_rank = record.rank = prev_rank + prev_count
        prev_stars = record.stargazers_count
        prev_count = 1
      elsif record.stargazers_count == prev_stars
        record.rank = prev_rank
        prev_count += 1
      else
        raise 'Unexpected order of records'
      end
    end
  end

  private

  def calculate(record)
    resolve_sti(record).where('stargazers_count > ?', record.stargazers_count).count + 1
  end

  # Guess rank from cache, might return slightly wrong one
  def guess(record, rank_record)
    if rank_record.nil?
      Rails.logger.info("#{@rank_class} was missing for #{record.attributes.inspect}")
      0 # normally this does not happen
    elsif rank_record.stargazers_count == record.stargazers_count
      rank_record.rank # maybe true
    else
      rank_record.rank - 1 # just guess, but true in most cases
    end
  end

  def resolve_sti(record)
    if record.respond_to?(:type)
      record.class.where(type: record.type)
    else
      record.class
    end
  end
end
