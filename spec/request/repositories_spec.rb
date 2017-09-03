require 'rails_helper'

RSpec.describe 'repositories', type: :request do
  describe '#show' do
    it 'shows path with extension' do
      repo = FactoryGirl.create(
        :repository,
        name: 'animate.css',
        full_name: 'daneden/animate.css',
        owner: FactoryGirl.create(:user, login: 'daneden')
      )
      get "/#{repo.full_name}"
      expect(response.status).to eq(200)
    end
  end
end
