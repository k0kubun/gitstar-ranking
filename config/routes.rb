Rails.application.routes.draw do
  root to: 'top#index'

  devise_for :users

  resources :repositories, only: :index

  resources :users, only: :index do
    collection do
      get :orgs
    end
  end

  resource :search, only: :show

  resources :users, only: :show, param: :login, path: '/' do
    resources :repositories, only: :show, param: :name, path: '/'
  end
end
