class AddFetchedAtToRepositories < ActiveRecord::Migration
  def change
    add_column :repositories, :fetched_at, :datetime
  end
end
