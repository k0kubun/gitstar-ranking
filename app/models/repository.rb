class Repository < ActiveRecord::Base
  include Concerns::Starrable

  belongs_to :owner, class_name: 'User'
end
