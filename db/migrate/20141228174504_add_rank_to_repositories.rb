class AddRankToRepositories < ActiveRecord::Migration
  def change
    add_column :repositories, :rank, :integer
    add_index :repositories, :rank
  end
end
