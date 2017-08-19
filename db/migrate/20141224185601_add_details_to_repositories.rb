class AddDetailsToRepositories < ActiveRecord::Migration[4.2]
  def change
    add_column :repositories, :homepage, :string
    add_column :repositories, :stargazers_count, :integer, null: false, default: 0
    add_column :repositories, :language, :string
  end
end
