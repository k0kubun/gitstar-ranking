class LastUpdate < ApplicationRecord
  belongs_to :user, foreign_key: :cursor
end
