class ChangeIndexesOnUsersAndRepositoriesAgain < ActiveRecord::Migration[4.2]
  def change
    add_index :users, [:type, :stargazers_count]
    remove_index :users, [:type, :stargazers_count, :id]
    add_index :repositories, :stargazers_count
    remove_index :repositories, [:stargazers_count, :id]
  end
end
