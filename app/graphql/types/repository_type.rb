Types::RepositoryType = GraphQL::ObjectType.define do
  name "Repository"
  field :name, types.String
  field :fullName, types.String
  field :description, types.String
  field :fork, types.Boolean
  field :createdAt, types.String
  field :updatedAt, types.String
  field :homepage, types.String
  field :stargazersCount, types.Int
  field :language, types.String
  field :fetchedAt, types.String
end
