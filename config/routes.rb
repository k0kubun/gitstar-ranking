Rails.application.routes.draw do
  root to: 'top#index'

  devise_for :users, controllers: { omniauth_callbacks: :omniauth_callbacks }

  devise_scope :user do
    namespace :devise do
      resource :session, only: :destroy
    end
  end

  resources :users, only: :index
  resources :organizations, only: :index
  resources :repositories, only: :index

  resource :search, only: :show

  resources :users, only: :show, param: :login, path: '/' do
    resources :repositories, only: :show, param: :name, path: '/'
  end
end
