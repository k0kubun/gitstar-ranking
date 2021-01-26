Rails.application.routes.draw do
  root to: 'top#index'

  authenticate :user do
    post '/graphql', to: 'graphql#execute'
    mount GraphiQL::Rails::Engine, at: '/graphiql', graphql_path: '/graphql'
  end

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
      get :shields
      post :update_later, on: :member
      post :bulk_update, on: :collection
    end
  end

  resources :users, only: :index
  resources :organizations, only: :index
  resources :repositories, only: :index

  resource :search, only: :show

  resource :site_map, only: :show

  resources :users, only: :show, param: :login, constraints: { login: /[a-zA-Z0-9-]+/ }, path: '/' do
    resources :repositories, only: :show, param: :name, constraints: { name: /[^\/]+/ }, path: '/', format: :html
  end
end
