class DropQueuedAtIndexFromUsers < ActiveRecord::Migration[4.2]
  def change
    remove_index :users, :queued_at
  end
end
