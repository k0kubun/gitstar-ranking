class CreateAccessTokens < ActiveRecord::Migration
  def change
    create_table :access_tokens do |t|
      t.integer :user_id
      t.string :token

      t.timestamps null: false
    end
    add_index :access_tokens, :user_id
  end
end
