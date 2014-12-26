class Repository < ActiveRecord::Base
  belongs_to :owner, class_name: 'User'

  def self.owned_by?(user_id)
    where(owner_id: user_id).limit(1).pluck('1 AS one').present?
  end
end
