class AddIndexOnStargazersCountToRepositories < ActiveRecord::Migration[4.2]
  def change
    add_index :repositories, :stargazers_count
  end
end
