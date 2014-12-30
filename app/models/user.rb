class User < ActiveRecord::Base
  include Concerns::Starrable

  # NOTE: COUNT query is a slow query
  TOTAL_USERS = 9489559

  paginates_per 100

  devise :omniauthable, omniauth_providers: [:github]

  validates :login, presence: true
  validates :type, presence: true

  has_many :repositories, foreign_key: :owner_id

  scope :not_organization, -> { where(type: 'User') }

  searchable do
    text :login
    text :type
    integer :stargazers_count
  end

  def to_param
    login
  end
end
