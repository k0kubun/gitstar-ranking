require 'rails_helper'

RSpec.describe 'repositories', type: :request do
  describe '#show' do
    it 'renders .css path with proper Content-Type' do
      repo = FactoryGirl.create(
        :repository,
        name: 'animate.css',
        full_name: 'daneden/animate.css',
        owner: FactoryGirl.create(:user, login: 'daneden'),
      )
      get "/#{repo.full_name}"
      expect(response.status).to eq(200)
      expect(response.headers['Content-Type']).to match(%r[\Atext/html;])
    end

    it 'renders .js path with proper Content-Type' do
      repo = FactoryGirl.create(
        :repository,
        name: 'angular.js',
        full_name: 'angular/angular.js',
        owner: FactoryGirl.create(:user, login: 'angular'),
      )
      get "/#{repo.full_name}"
      expect(response.status).to eq(200)
      expect(response.headers['Content-Type']).to match(%r[\Atext/html;])
    end
  end
end
