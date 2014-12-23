Rails.application.routes.draw do
  root to: 'users#index'

  resources :repos, only: :index
end
