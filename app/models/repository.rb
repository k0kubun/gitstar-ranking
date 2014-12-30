class Repository < ActiveRecord::Base
  include Concerns::Starrable

  belongs_to :owner, class_name: 'User'

  searchable do
    text :name
    text :full_name
    integer :stargazers_count
  end

  def to_param
    name
  end
end
