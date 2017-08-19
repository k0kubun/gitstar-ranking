class DropRankFromUsers < ActiveRecord::Migration
  def change
    remove_column :users, :rank, :integer
    remove_column :repositories, :rank, :integer
  end
end
