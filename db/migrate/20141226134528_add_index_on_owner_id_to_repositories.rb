class AddIndexOnOwnerIdToRepositories < ActiveRecord::Migration[4.2]
  def change
    add_index :repositories, :owner_id
  end
end
