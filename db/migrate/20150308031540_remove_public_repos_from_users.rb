class RemovePublicReposFromUsers < ActiveRecord::Migration
  def change
    remove_index :users, :public_repos
    remove_column :users, :public_repos, :integer
  end
end
