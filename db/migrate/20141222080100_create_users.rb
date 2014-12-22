class CreateUsers < ActiveRecord::Migration
  def change
    create_table :users do |t|
      t.string :login
      t.string :avatar_url
      t.string :type
      t.boolean :site_admin

      t.timestamps null: false
    end
  end
end
