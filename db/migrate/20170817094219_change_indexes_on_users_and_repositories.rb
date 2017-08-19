class ChangeIndexesOnUsersAndRepositories < ActiveRecord::Migration[4.2]
  def change
    remove_index :users, :rank
    add_index :users, [:type, :stargazers_count, :id]
    remove_index :users, :stargazers_count
    remove_index :repositories, :rank
    add_index :repositories, [:stargazers_count, :id]
    remove_index :repositories, :stargazers_count
  end
end
