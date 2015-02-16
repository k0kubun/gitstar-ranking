class Repository < ActiveRecord::Base
  include Concerns::Rankable
  include Concerns::Starrable

  self.minimum_rankable_star = 15

  paginates_per 100

  belongs_to :owner, class_name: 'User'

  def to_param
    name
  end
end
