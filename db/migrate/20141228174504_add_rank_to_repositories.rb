class AddRankToRepositories < ActiveRecord::Migration[4.2]
  def change
    add_column :repositories, :rank, :integer
    add_index :repositories, :rank
  end
end
