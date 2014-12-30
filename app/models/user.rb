class User < ActiveRecord::Base
  include Concerns::Starrable

  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable and :omniauthable
  devise :omniauthable, omniauth_providers: [:github]

  validates :login, presence: true
  validates :type, presence: true

  has_many :repositories, foreign_key: :owner_id

  scope :not_organization, -> { where(type: 'User') }

  searchable do
    text :login
  end

  def to_param
    login
  end
end
