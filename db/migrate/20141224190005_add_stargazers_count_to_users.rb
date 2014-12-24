class AddStargazersCountToUsers < ActiveRecord::Migration
  def change
    add_column :users, :stargazers_count, :integer, null: false, default: 0
  end
end
