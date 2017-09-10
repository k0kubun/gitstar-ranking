Types::MutationType = GraphQL::ObjectType.define do
  name 'Mutation'

  field :updateUser, Types::UpdateUserJobType do
    description 'An example field added by the generator'
    argument :login, types.String

    resolve ->(obj, args, ctx) {
      unless args.key?('login')
        return GraphQL::ExecutionError.new('Argument "login" was missing for updateUser field')
      end

      unless user = User.find_by(login: args['login'])
        return GraphQL::ExecutionError.new("User not found for login #{args['login'].dump}")
      end

      UpdateUserJob.perform_later(user_id: user.id, token_user_id: ctx[:current_user].id)
    }
  end
end
