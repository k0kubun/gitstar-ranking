require 'sidekiq/web'

Rails.application.routes.draw do
  root to: 'users#index'

  mount Sidekiq::Web, at: '/sidekiq'

  resources :users, only: [] do
    collection do
      get :orgs
    end
  end

  resources :repositories, only: :index
end
