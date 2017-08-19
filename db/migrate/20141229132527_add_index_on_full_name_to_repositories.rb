class AddIndexOnFullNameToRepositories < ActiveRecord::Migration[4.2]
  def change
    add_index :repositories, :full_name
  end
end
