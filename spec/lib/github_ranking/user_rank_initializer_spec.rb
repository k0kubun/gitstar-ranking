require 'rails_helper'

describe GithubRanking::UserRankInitializer do
  describe '#run' do
    it 'initializes UserRank records' do
      stub_const('GithubRanking::UserRankInitializer::BATCH_SIZE', 2)

      FactoryGirl.create(:user, stargazers_count: 2)
      FactoryGirl.create(:user, stargazers_count: 1)
      FactoryGirl.create(:user, stargazers_count: 2)

      GithubRanking::UserRankInitializer.run

      rank1 = UserRank.find_by(stargazers_count: 1)
      expect(rank1.records_count).to eq(1)
      expect(rank1.rank).to eq(3)
      rank2 = UserRank.find_by(stargazers_count: 2)
      expect(rank2.records_count).to eq(2)
      expect(rank2.rank).to eq(1)
    end
  end
end
