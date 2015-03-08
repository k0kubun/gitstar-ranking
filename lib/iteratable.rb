module Iteratable
  BATCH_SIZE = 5000

  def iterate_all(relation, columns, last_id = 0, debug = false, &block)
    assert_first_column(columns, :id)

    loop do
      records = relation.dup.where('id > ?', last_id).
        limit(BATCH_SIZE).order(id: :asc).pluck(*columns)
      break if records.blank?

      records.each do |*args|
        block.call(*args)
      end
      last_id =
        if columns.size > 1
          records.last.first
        else
          records.last
        end
      print "#{last_id}\r" if debug
    end
  end

  private

  def assert_first_column(columns, column)
    if columns.first != column
      raise ArgumentError, "First column must be #{column}"
    end
  end
end
