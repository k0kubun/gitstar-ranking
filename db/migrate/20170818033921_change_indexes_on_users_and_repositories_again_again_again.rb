class ChangeIndexesOnUsersAndRepositoriesAgainAgainAgain < ActiveRecord::Migration
  def change
    remove_index :users, [:type, :stargazers_count]
  end
end
