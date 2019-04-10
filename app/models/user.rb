# id, login, avatar_url, type should be given from API.
class User < ApplicationRecord
  IMPORT_ATTRIBUTES = %i[id login avatar_url type]
  ADMIN_IDS = [
    3138447, # k0kubun
  ].freeze

  attr_writer :rank

  PER_PAGE = 100
  paginates_per PER_PAGE

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

  def not_queued_for_last?(duration)
    return true if queued_at.nil?
    queued_at < (Time.now - duration)
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
    @rank ||= RankBuilder.new(UserRank).build(self)
  end

  def rate_limit
    client = GithubApi::V4Client.new(access_token.token)
    client.rate_limit
  end

  def member_of?(organization_login)
    return false if access_token&.token.nil?
    client = GithubApi::V3Client.new(access_token.token)
    client.organization_member?(organization_login, login)
  end

  private

  def update_threshold
    Time.now - 10.minutes
  end
end
