Rails.application.routes.draw do
  root to: 'top#index'

  resources :repositories, only: :index

  resources :users, only: :index do
    collection do
      get :orgs
    end
  end

  resources :users, only: :show, param: :login, path: '/'
end
