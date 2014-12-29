class AddIndexOnFullNameToRepositories < ActiveRecord::Migration
  def change
    add_index :repositories, :full_name
  end
end
