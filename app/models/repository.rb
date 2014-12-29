class Repository < ActiveRecord::Base
  include Concerns::Starrable

  belongs_to :owner, class_name: 'User'

  searchable do
    text :name
    text :full_name
  end
end
