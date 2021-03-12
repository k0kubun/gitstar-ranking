class LastUpdate < ApplicationRecord
  belongs_to :user, foreign_key: :cursor

  def self.full_user = find_by(id: 1) # UserFullScanWorker user_id
  def self.star_user = find_by(id: 2) # UserStarScanWorker user_id
  def self.star_star = find_by(id: 3) # UserStarScanWorker stargazers_count
end
