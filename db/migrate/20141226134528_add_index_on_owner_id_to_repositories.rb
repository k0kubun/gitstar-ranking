class AddIndexOnOwnerIdToRepositories < ActiveRecord::Migration
  def change
    add_index :repositories, :owner_id
  end
end
