Rails.application.routes.draw do
  root to: 'users#index'

  resources :users, only: [] do
    collection do
      get :orgs
    end
  end

  resources :repositories, only: :index
end
