class User < ActiveRecord::Base
  include Concerns::Starrable

  validates :login, presence: true
  validates :type, presence: true

  has_many :repositories, foreign_key: :owner_id

  scope :not_organization, -> { where(type: 'User') }
end
