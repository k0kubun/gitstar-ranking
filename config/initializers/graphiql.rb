GraphiQL::Rails.config.initial_query = <<~GRAPHQL
  query {
    __schema {
      types {
        name
        kind
        description
        fields {
          name
        }
      }
    }
  }
GRAPHQL
