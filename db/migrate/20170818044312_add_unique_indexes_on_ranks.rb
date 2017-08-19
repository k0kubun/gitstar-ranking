class AddUniqueIndexesOnRanks < ActiveRecord::Migration[4.2]
  def change
    add_index :user_ranks, :rank, unique: true
    add_index :organization_ranks, :rank, unique: true
    add_index :repository_ranks, :rank, unique: true
  end
end
