Rails.application.routes.draw do
  root to: 'top#index'

  resources :users, only: [] do
    collection do
      get :orgs
    end
  end

  resources :repositories, only: :index
end
