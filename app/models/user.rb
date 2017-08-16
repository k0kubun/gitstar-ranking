class User < ActiveRecord::Base
  IMPORT_ATTRIBUTES = %i[id login avatar_url type]
  ADMIN_IDS = [
    3138447, # k0kubun
  ].freeze

  class_attribute :ranking_key
  self.ranking_key = 'github-ranking:user:world:all'

  paginates_per 100

  devise :omniauthable, omniauth_providers: [:github]

  validates :login, presence: true
  validates :type, presence: true
  validates :avatar_url, presence: true

  has_one :access_token
  has_many :repositories, -> { starred_first }, foreign_key: :owner_id

  scope :starred_first, -> { order(stargazers_count: :desc) }
  scope :organization, -> { where(type: 'Organization') }
  scope :not_organization, -> { where(type: 'User') }

  def to_param
    login
  end

  def admin?
    ADMIN_IDS.include?(id)
  end

  def queued_recently?
    return false if queued_at.nil?
    update_threshold < queued_at
  end

  def in_queue?
    return false if queued_at.nil?
    update_threshold <= queued_at && updated_at <= queued_at
  end

  def rank
    Redis.current.zcount(ranking_key, "(#{stargazers_count}", '+inf') + 1
  rescue Redis::CannotConnectError
    super
  end

  private

  def update_threshold
    Time.now - 10.minutes
  end
end
