class AddIndexOnQueuedAtToUsers < ActiveRecord::Migration
  def change
    add_index :users, :queued_at
  end
end
