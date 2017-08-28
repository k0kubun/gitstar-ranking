class ApplicationRecord < ActiveRecord::Base
  self.abstract_class = true

  # For CLI usage, not used by app
  def graphql_id
    Base64.encode64("04:#{self.class.name}#{id}").rstrip
  end
end
