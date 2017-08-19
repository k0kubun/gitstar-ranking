class AddIndexOnStargazersCountToUsers < ActiveRecord::Migration[4.2]
  def change
    add_index :users, :stargazers_count
  end
end
