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

  field :repository do
    type Types::RepositoryType
    argument :fullName, types.String
    resolve ->(obj, args, ctx) {
      unless args.key?('fullName')
        return GraphQL::ExecutionError.new('Argument "fullName" was missing for repository field')
      end

      unless repo = Repository.find_by(full_name: args['fullName'])
        return GraphQL::ExecutionError.new("Repository not found for fullName #{args['fullName'].dump}")
      end

      repo
    }
  end
end
