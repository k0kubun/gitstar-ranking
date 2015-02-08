require 'rails_helper'

describe AccessToken do
  let!(:access_token) { FactoryGirl.create(:access_token) }
  let(:limit) { 5000 }
  let(:remaining) { 4998 }

  before do
    Rails.cache.clear
  end

  describe '#rate_limit' do
    subject { access_token.rate_limit }

    let(:client) { double(Octokit::Client) }

    before do
      allow(access_token).to receive(:client).and_return(client)
    end

    context 'with authorized' do
      let(:rate_limit) do
        double(Octokit::RateLimit, limit: limit, remaining: remaining)
      end

      before do
        allow(client).to receive(:rate_limit).and_return(rate_limit)
      end

      it 'returns rate limit hash' do
        expect(subject).to eq({ limit: limit, remaining: remaining })
      end
    end

    context 'with unauthorized' do
      before do
        allow(client).to receive(:rate_limit).and_raise(Octokit::Unauthorized)
      end

      it 'returns hash with zero' do
        expect(subject).to eq({ limit: 0, remaining: 0 })
      end
    end
  end
end
