class User < ActiveRecord::Base
  include Concerns::Rankable
  include Concerns::Starrable

  IMPORT_ATTRIBUTES = %i[id login avatar_url type]
  ADMIN_IDS = [
    3138447, # k0kubun
  ].freeze

  self.ranking_key = 'githubranking:user:world:all'
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
  scope :queued_last, -> { order(queued_at: :asc) }
  scope :queued_last, -> { order(queued_at: :asc) }

  def self.import_updates(updates)
    users = []

    queued_at = Time.now
    Array.wrap(updates).each do |update|
      users << IMPORT_ATTRIBUTES.map { |attr| update[attr] } + [queued_at]
    end

    self.import(
      IMPORT_ATTRIBUTES + [:queued_at],
      users,
      on_duplicate_key_update: IMPORT_ATTRIBUTES[1..-1] + [:queued_at],
      validate: false,
    )
  end

  def to_param
    login
  end

  def admin?
    ADMIN_IDS.include?(id)
  end

  def queued_recently?
    update_threshold < queued_at
  end

  def in_queue?
    update_threshold <= queued_at && updated_at <= queued_at
  end

  private

  def update_threshold
    Time.now - 10.minutes
  end
end
