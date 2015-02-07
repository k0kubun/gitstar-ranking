require 'rails_helper'

describe RankingUpdater do
  let(:job) { described_class.new(User, :not_organization) }
  let!(:user_1) { FactoryGirl.create(:user, rank: 0, stargazers_count: 3) }
  let!(:user_2) { FactoryGirl.create(:user, rank: 0, stargazers_count: 2) }
  let!(:user_3) { FactoryGirl.create(:user, rank: 0, stargazers_count: 2) }
  let!(:user_4) { FactoryGirl.create(:user, rank: 0, stargazers_count: 1) }

  describe '#run' do
    context 'when threshold is larger than user count' do
      it 'ranks up properly for the same star counts' do
        expect {
          job.run(logic_change_threshold: 5, min_star: 0)
        }.to change {
          (1..4).map { |n| self.public_send(:"user_#{n}").reload.rank }
        }.from([0, 0, 0, 0]).to([1, 2, 2, 4])
      end
    end

    context 'when threshold is less than user count and larger than 0' do
      it 'ranks up properly for the same star counts' do
        expect {
          job.run(logic_change_threshold: 1, min_star: 0)
        }.to change {
          (1..4).map { |n| self.public_send(:"user_#{n}").reload.rank }
        }.from([0, 0, 0, 0]).to([1, 2, 2, 4])
      end
    end

    context 'when min_star is larger than 0' do
      it 'does not rank up records whose star count is under than min_star' do
        expect {
          job.run(logic_change_threshold: 2, min_star: 2)
        }.to change {
          (1..4).map { |n| self.public_send(:"user_#{n}").reload.rank }
        }.from([0, 0, 0, 0]).to([1, 2, 2, 0])
      end
    end
  end
end
