Types::UserType = GraphQL::ObjectType.define do
  name "User"
  field :id, types.Int
  field :login, types.String
  field :avatar_url, types.String
  field :type, types.String
  field :created_at, types.String
  field :updated_at, types.String
  field :stargazers_count, types.Int
  field :location, types.String
end
