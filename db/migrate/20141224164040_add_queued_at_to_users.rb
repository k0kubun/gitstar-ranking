class AddQueuedAtToUsers < ActiveRecord::Migration
  def change
    add_column :users, :queued_at, :datetime
  end
end
