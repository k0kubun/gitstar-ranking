require 'sidekiq/web'

Rails.application.routes.draw do
  root to: 'top#index'

  authenticate :user, ->(u) { u.admin? } do
    mount Sidekiq::Web, at: '/sidekiq_admin'

    resources :access_tokens, only: :index
  end

  devise_for :users, controllers: { omniauth_callbacks: :omniauth_callbacks }

  devise_scope :user do
    namespace :devise do
      resource :session, only: :destroy
    end

    resource :user, only: [], path: '/' do
      post :update_myself, on: :member
    end

    resource :user, only: [], path: '/' do
      post :update_org, on: :member
    end
  end

  resources :users, only: :index
  resources :organizations, only: :index
  resources :repositories, only: :index

  resource :search, only: :show

  resource :site_map, only: :show

  resources :users, only: :show, param: :login, constraints: { login: /[a-zA-Z0-9-]+/ }, path: '/' do
    resources :repositories, only: :show, param: :name, constraints: { name: /[^\/]+/ }, path: '/'
  end
end
