# frozen_string_literal: true
require 'json'
require 'net/http'
require 'uri'

class GithubApi::V4::Client
  ENDPOINT = URI.parse('https://api.github.com')
  ClientError = Class.new(StandardError)
  ServerError = Class.new(StandardError)

  # @param [String] access_token
  def initialize(access_token)
    @access_token = access_token
  end

  # @param [String] query
  # @param [Hash] variables
  # @return [Hash]
  def graphql(query:, variables: {})
    resp = post('/graphql', query: query, variables: variables)
    handle_errors(resp)
    JSON.parse(resp.body)
  end

  # @return [Hash]
  def schema
    graphql(query: <<~GRAPHQL)
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
  end

  # @param [String] name
  # @return [Hash]
  def type(name)
    graphql(query: <<~GRAPHQL)
      query {
        __type(name: #{name.dump}) {
          name
          kind
          description
          fields {
            name
          }
        }
      }
    GRAPHQL
  end

  private

  def handle_errors(resp)
    case resp
    when Net::HTTPClientError
      raise ClientError.new("#{resp.code}: #{resp.body}")
    when Net::HTTPServerError
      raise ServerError.new("#{resp.code}: #{resp.body}")
    end
  end

  def post(path, params = {})
    Net::HTTP.start(ENDPOINT.host, ENDPOINT.port, use_ssl: ENDPOINT.scheme == 'https') do |http|
      headers = {
        'Authorization': "bearer #{@access_token}",
        'Content-Type':  'application/json',
      }
      http.post(path, params.to_json, headers)
    end
  end
end
