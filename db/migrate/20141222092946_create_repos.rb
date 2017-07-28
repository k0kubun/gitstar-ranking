class CreateRepos < ActiveRecord::Migration
  def change
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
