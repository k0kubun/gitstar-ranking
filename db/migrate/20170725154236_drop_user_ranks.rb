class DropUserRanks < ActiveRecord::Migration
  def up
    drop_table :user_ranks
  end

  def down
    create_table :user_ranks do |t|
      t.integer :stargazers_count, null: false
      t.integer :records_count, null: false
      t.integer :rank

      t.timestamps null: false
      t.index :stargazers_count, unique: true
    end
  end
end
