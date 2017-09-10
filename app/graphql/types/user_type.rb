Types::UserType = GraphQL::ObjectType.define do
  name 'User'

  field :id, types.Int
  field :login, types.String
  field :avatarUrl, types.String, property: :avatarUrl
  field :type, types.String
  field :createdAt, types.String, property: :created_at
  field :updatedAt, types.String, property: :updated_at
  field :stargazersCount, types.Int, property: :stargazers_count
  field :location, types.String
  field :updateStatus, types.String, 'OUTDATED, UPDATED, UPDATING' do
    resolve ->(user, args, ctx) {
      if user.in_queue?
        'UPDATING'
      elsif user.queued_recently?
        'UPDATED'
      else
        'OUTDATED'
      end
    }
  end
end
