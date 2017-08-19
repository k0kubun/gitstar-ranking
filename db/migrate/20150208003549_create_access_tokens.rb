class CreateAccessTokens < ActiveRecord::Migration[4.2]
  def change
    create_table :access_tokens, options: 'ENGINE=InnoDB ROW_FORMAT=dynamic DEFAULT CHARSET=utf8mb4' do |t|
      t.integer :user_id
      t.string :token

      t.timestamps null: false
    end
    add_index :access_tokens, :user_id
  end
end
