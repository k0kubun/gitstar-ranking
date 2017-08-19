class AddIndexOnQueuedAtToUsers < ActiveRecord::Migration[4.2]
  def change
    add_index :users, :queued_at
  end
end
