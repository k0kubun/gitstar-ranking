Rails.application.routes.draw do
  root to: 'top#index'

  authenticate :user, ->(u) { u.admin? } do
    resources :access_tokens, only: :index do
      collection do
        get :answered
      end
    end
  end

  devise_for :users, controllers: { omniauth_callbacks: :omniauth_callbacks }

  devise_scope :user do
    namespace :devise do
      resource :session, only: :destroy
    end

    resources :users, only: [], param: :login, constraints: { login: /[a-zA-Z0-9-]+/ } do
      get :update_status, on: :member
      post :update_later, on: :member
      post :bulk_update, on: :collection
    end
  end

  resources :users, only: :index, param: :login, constraints: { login: /[a-zA-Z0-9-]+/ } do
    get :shields, on: :member
  end
  resources :organizations, only: :index
  resources :repositories, only: :index

  resource :search, only: :show

  resource :sitemap, only: :show

  resources :users, only: :show, param: :login, constraints: { login: /[a-zA-Z0-9-]+/ }, path: '/' do
    resources :repositories, only: :show, param: :name, constraints: { name: /[^\/]+/ }, path: '/', format: :html
  end
end
