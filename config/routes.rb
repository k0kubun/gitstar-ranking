require 'sidekiq/web'

Rails.application.routes.draw do
  constraints subdomain: 'sidekiq' do
    mount Sidekiq::Web, at: '/sidekiq'
  end

  root to: 'users#index'

  resources :users, only: [] do
    collection do
      get :orgs
    end
  end

  resources :repositories, only: :index
end
