class DropQueuedAtIndexFromUsers < ActiveRecord::Migration
  def change
    remove_index :users, :queued_at
  end
end
