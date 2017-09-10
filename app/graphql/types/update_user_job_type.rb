Types::UpdateUserJobType = GraphQL::ObjectType.define do
  name 'UpdateUserJob'
  field :payload, types.String
end
