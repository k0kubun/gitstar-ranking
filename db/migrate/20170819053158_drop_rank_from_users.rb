class DropRankFromUsers < ActiveRecord::Migration[4.2]
  def change
    remove_column :users, :rank, :integer
    remove_column :repositories, :rank, :integer
  end
end
