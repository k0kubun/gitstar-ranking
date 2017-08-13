class CreateUpdateUserJobs < ActiveRecord::Migration
  def change
    create_table :update_user_jobs do |t|
      t.string :payload, null: false
      t.datetime :timeout_at, null: false
      t.column :owner, 'bigint(21) unsigned', null: false, default: 0

      t.timestamps null: false
      t.index :timeout_at
    end
  end
end
