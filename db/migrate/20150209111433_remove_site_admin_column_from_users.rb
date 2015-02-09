class RemoveSiteAdminColumnFromUsers < ActiveRecord::Migration
  def change
    remove_column :users, :site_admin, :boolean
  end
end
