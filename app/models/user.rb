class User < ActiveRecord::Base
  validates :login, presence: true
  validates :type, presence: true
end
