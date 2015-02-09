class AddPublicReposToUsers < ActiveRecord::Migration
  def change
    add_column :users, :public_repos, :integer
    add_index :users, :public_repos
  end
end
