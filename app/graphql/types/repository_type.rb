Types::RepositoryType = GraphQL::ObjectType.define do
  name 'Repository'

  field :name, types.String
  field :fullName, types.String, property: :full_name
  field :description, types.String
  field :fork, types.Boolean
  field :createdAt, types.String, property: :created_at
  field :updatedAt, types.String, property: :updated_at
  field :homepage, types.String
  field :stargazersCount, types.Int, property: :stargazers_count
  field :language, types.String
  field :fetchedAt, types.String, property: :fetched_at

  field :owner, Types::OwnerType do
    resolve ->(repo, args, ctx) do
      GraphQL::RecordLoader.for(User).load(repo.owner_id).then(&:itself)
    end
  end
end
