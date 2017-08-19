class ChangeIndexesOnRepositories < ActiveRecord::Migration
  def change
    add_index :repositories, [:owner_id, :stargazers_count]
    remove_index :repositories, [:owner_id]
  end
end
