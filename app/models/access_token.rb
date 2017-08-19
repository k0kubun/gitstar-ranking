class AccessToken < ApplicationRecord
  TOKEN_LIST_KEY = 'access-token-list'
  TOKEN_LIMIT = 100

  paginates_per 50

  belongs_to :user
end
