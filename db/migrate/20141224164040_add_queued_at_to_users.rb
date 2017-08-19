class AddQueuedAtToUsers < ActiveRecord::Migration[4.2]
  def change
    add_column :users, :queued_at, :datetime
  end
end
