class AddIndexOnLoginToUsers < ActiveRecord::Migration[4.2]
  def change
    add_index :users, :login
  end
end
