class GraphQL::RecordLoader < GraphQL::Batch::Loader
  # @param [Class] model
  def initialize(model)
    @model = model
  end

  def perform(ids)
    @model.where(id: ids).each do |record|
      fulfill(record.id, record)
    end
    ids.each { |id| fulfill(id, nil) unless fulfilled?(id) }
  end
end
