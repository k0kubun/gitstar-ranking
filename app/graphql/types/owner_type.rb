# Repository-nested User type to avoid circular reference
Types::OwnerType = GraphQL::ObjectType.define do
  name 'Owner'

  field :id, types.Int
  field :login, types.String
  field :avatarUrl, types.String, property: :avatarUrl
  field :type, types.String
  field :createdAt, types.String, property: :created_at
  field :updatedAt, types.String, property: :updated_at
  field :stargazersCount, types.Int, property: :stargazers_count
  field :location, types.String
end
