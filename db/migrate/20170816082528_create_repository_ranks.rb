class CreateRepositoryRanks < ActiveRecord::Migration
  def change
    create_table :repository_ranks do |t|
      t.integer :stargazers_count, null: false
      t.integer :rank, null: false

      t.timestamps null: false
      t.index :stargazers_count, unique: true
    end
  end
end
