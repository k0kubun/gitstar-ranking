class CreateRepositories < ActiveRecord::Migration[4.2]
  def up
    drop_table :repos
    create_table :repositories, options: 'ENGINE=InnoDB ROW_FORMAT=dynamic DEFAULT CHARSET=utf8mb4' do |t|
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
    create_table :repos, options: 'ENGINE=InnoDB ROW_FORMAT=dynamic DEFAULT CHARSET=utf8mb4' do |t|
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
