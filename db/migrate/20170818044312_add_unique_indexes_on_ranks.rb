class AddUniqueIndexesOnRanks < ActiveRecord::Migration
  def change
    add_index :user_ranks, :rank, unique: true
    add_index :organization_ranks, :rank, unique: true
    add_index :repository_ranks, :rank, unique: true
  end
end
