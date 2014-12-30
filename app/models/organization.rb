class Organization < User
  paginates_per 100

  searchable do
    text :login
    text :type
    integer :stargazers_count
  end
end
