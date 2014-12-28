class AddRankToUsers < ActiveRecord::Migration
  def change
    add_column :users, :rank, :integer
    add_index :users, :rank
  end
end
