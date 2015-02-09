class User < ActiveRecord::Base
  include Concerns::Rankable
  include Concerns::Starrable

  ADMIN_IDS = [
    3138447, # k0kubun
  ].freeze

  self.minimum_rankable_star = 1

  paginates_per 100

  devise :omniauthable, omniauth_providers: [:github]

  validates :login, presence: true
  validates :type, presence: true
  validates :avatar_url, presence: true

  has_one :access_token
  has_many :repositories, -> { starred_first }, foreign_key: :owner_id

  scope :organization, -> { where(type: 'Organization') }
  scope :not_organization, -> { where(type: 'User') }

  searchable(auto_index: false) do
    text :login
    integer :stargazers_count
    boolean :user_flag do
      self.type == 'User'
    end
  end

  def to_param
    login
  end

  def admin?
    ADMIN_IDS.include?(id)
  end

  def queued_recently?
    queued_at > 10.minutes.ago
  end

  def in_queue?
    10.minutes.ago <= queued_at && updated_at <= queued_at
  end
end
