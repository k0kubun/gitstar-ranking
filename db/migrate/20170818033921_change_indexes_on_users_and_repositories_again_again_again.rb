class ChangeIndexesOnUsersAndRepositoriesAgainAgainAgain < ActiveRecord::Migration[4.2]
  def change
    remove_index :users, [:type, :stargazers_count]
  end
end
