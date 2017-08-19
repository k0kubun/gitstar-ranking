class CreateUsers < ActiveRecord::Migration[4.2]
  def change
    create_table :users, options: 'ENGINE=InnoDB ROW_FORMAT=dynamic DEFAULT CHARSET=utf8mb4' do |t|
      t.string :login
      t.string :avatar_url
      t.string :type
      t.boolean :site_admin

      t.timestamps null: false
    end
  end
end
