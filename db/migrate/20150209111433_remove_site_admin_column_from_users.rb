class RemoveSiteAdminColumnFromUsers < ActiveRecord::Migration[4.2]
  def change
    remove_column :users, :site_admin, :boolean
  end
end
