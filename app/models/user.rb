class User < ActiveRecord::Base
  validates :login, presence: true
  validates :type, presence: true

  has_many :repositories, foreign_key: :owner_id
end
