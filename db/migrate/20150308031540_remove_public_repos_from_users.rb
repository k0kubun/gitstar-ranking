class RemovePublicReposFromUsers < ActiveRecord::Migration[4.2]
  def change
    remove_index :users, :public_repos
    remove_column :users, :public_repos, :integer
  end
end
