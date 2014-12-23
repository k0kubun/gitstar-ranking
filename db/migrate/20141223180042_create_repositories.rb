class CreateRepositories < ActiveRecord::Migration
  def up
    drop_table :repos
    create_table :repositories do |t|
      t.string :name
      t.string :full_name
      t.integer :owner_id
      t.text :description
      t.boolean :fork

      t.timestamps null: false
    end
  end

  def down
    drop_table :repositories
    create_table :repos do |t|
      t.string :name
      t.string :full_name
      t.integer :owner_id
      t.text :description
      t.string :homepage
      t.integer :stargazers_count
      t.string :language

      t.timestamps null: false
    end
  end
end
