class AddFetchedAtToRepositories < ActiveRecord::Migration[4.2]
  def change
    add_column :repositories, :fetched_at, :datetime
  end
end
