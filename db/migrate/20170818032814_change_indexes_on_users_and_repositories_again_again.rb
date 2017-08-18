class ChangeIndexesOnUsersAndRepositoriesAgainAgain < ActiveRecord::Migration
  def change
    add_index :users, :stargazers_count
  end
end
