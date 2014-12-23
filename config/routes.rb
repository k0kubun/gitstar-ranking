Rails.application.routes.draw do
  root to: 'users#index'

  resources :repositories, only: :index
end
