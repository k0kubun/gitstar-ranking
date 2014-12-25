class AddIndexOnStargazersCountToRepositories < ActiveRecord::Migration
  def change
    add_index :repositories, :stargazers_count
  end
end
