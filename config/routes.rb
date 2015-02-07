require 'sidekiq/web'

Rails.application.routes.draw do
  root to: 'top#index'

  authenticate :user, ->(u) { u.admin? } do
    mount Sidekiq::Web, at: '/sidekiq_admin'
  end

  devise_for :users, controllers: { omniauth_callbacks: :omniauth_callbacks }

  devise_scope :user do
    namespace :devise do
      resource :session, only: :destroy
    end
  end

  resources :users, only: :index
  resources :organizations, only: :index
  resources :repositories, only: :index

  resource :search, only: :show do
    member do
      get :fetch_count
    end
  end

  resources :users, only: :show, param: :login, path: '/' do
    resources :repositories, only: :show, param: :name,
      constraints: { name: /[^\/]+/ }, path: '/'
  end
end
