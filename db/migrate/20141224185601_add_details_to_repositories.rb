class AddDetailsToRepositories < ActiveRecord::Migration
  def change
    add_column :repositories, :homepage, :string
    add_column :repositories, :stargazers_count, :integer, null: false, default: 0
    add_column :repositories, :language, :string
  end
end
