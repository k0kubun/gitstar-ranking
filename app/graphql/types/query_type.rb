Types::QueryType = GraphQL::ObjectType.define do
  name 'Query'

  field :user do
    type Types::UserType
    argument :login, types.String
    resolve ->(obj, args, ctx) {
      unless args.key?('login')
        return GraphQL::ExecutionError.new('Argument "login" was missing for user field')
      end

      unless user = User.find_by(login: args['login'])
        return GraphQL::ExecutionError.new("User not found for login #{args['login'].dump}")
      end

      user
    }
  end
end
