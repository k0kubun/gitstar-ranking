class AddRankToUsers < ActiveRecord::Migration[4.2]
  def change
    add_column :users, :rank, :integer
    add_index :users, :rank
  end
end
