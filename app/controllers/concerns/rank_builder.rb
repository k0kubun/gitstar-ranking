class RankBuilder
  # @param [ActiveRecord::Base] rank_class - with stargazers_count, rank columns
  def initialize(rank_class)
    @rank_class = rank_class
  end

  # @param [ActiveRecord::Base] record
  def build(record)
    rank_record = @rank_class.where('stargazers_count <= ?', record.stargazers_count).order(stargazers_count: :desc).first
    calculate(record, rank_record)
  end

  # @param [ActiveRecord::Relation,Array<ActiveRecord::Base>] records
  def preload(records)
    rank_records = @rank_class.where(stargazers_count: records.map(&:stargazers_count)).order(stargazers_count: :desc)
    records.each do |record|
      rank_record = rank_records.find { |r| r.stargazers_count <= record.stargazers_count }
      record.rank = calculate(record, rank_record)
    end
  end

  private

  def calculate(record, rank_record)
    if rank_record.nil?
      Rails.logger.info("#{@rank_class} was missing for #{record.attributes.inspect}")
      0 # normally this does not happen
    elsif rank_record.stargazers_count == record.stargazers_count
      rank_record.rank # maybe true
    else
      rank_record.rank - 1 # just guess, but true in most cases
    end
  end
end
