require 'rails_helper'

describe User do
  let(:mojombo) do
    {
      login: "mojombo",
      id: 1,
      avatar_url: "https://avatars.githubusercontent.com/u/1?v=3",
      gravatar_id: "",
      url: "https://api.github.com/users/mojombo",
      html_url: "https://github.com/mojombo",
      followers_url: "https://api.github.com/users/mojombo/followers",
      following_url: "https://api.github.com/users/mojombo/following{/other_user}",
      gists_url: "https://api.github.com/users/mojombo/gists{/gist_id}",
      starred_url: "https://api.github.com/users/mojombo/starred{/owner}{/repo}",
      subscriptions_url: "https://api.github.com/users/mojombo/subscriptions",
      organizations_url: "https://api.github.com/users/mojombo/orgs",
      repos_url: "https://api.github.com/users/mojombo/repos",
      events_url: "https://api.github.com/users/mojombo/events{/privacy}",
      received_events_url: "https://api.github.com/users/mojombo/received_events",
      type: "User",
      site_admin: false,
      name: "Tom Preston-Werner",
      company: "",
      blog: "http://tom.preston-werner.com",
      location: "San Francisco",
      email: "tom@mojombo.com",
      hireable: false,
      bio: nil,
      public_repos: 58,
      public_gists: 65,
      followers: 17712,
      following: 11,
      created_at: Time.parse('2007-10-20 05:24:19 UTC'),
      updated_at: Time.parse('2015-02-10 22:52:27 UTC'),
    }
  end

  let(:defunkt) do
    {
      login: "defunkt",
      id: 2,
      avatar_url: "https://avatars.githubusercontent.com/u/2?v=3",
      gravatar_id: "",
      url: "https://api.github.com/users/defunkt",
      html_url: "https://github.com/defunkt",
      followers_url: "https://api.github.com/users/defunkt/followers",
      following_url: "https://api.github.com/users/defunkt/following{/other_user}",
      gists_url: "https://api.github.com/users/defunkt/gists{/gist_id}",
      starred_url: "https://api.github.com/users/defunkt/starred{/owner}{/repo}",
      subscriptions_url: "https://api.github.com/users/defunkt/subscriptions",
      organizations_url: "https://api.github.com/users/defunkt/orgs",
      repos_url: "https://api.github.com/users/defunkt/repos",
      events_url: "https://api.github.com/users/defunkt/events{/privacy}",
      received_events_url: "https://api.github.com/users/defunkt/received_events",
      type: "User",
      site_admin: true,
      name: "Chris Wanstrath",
      company: "GitHub",
      blog: "http://chriswanstrath.com/",
      location: "San Francisco",
      email: "chris@github.com",
      hireable: true,
      bio: nil,
      public_repos: 106,
      public_gists: 279,
      followers: 13893,
      following: 208,
      created_at: Time.parse('2007-10-20 05: 24: 19 UTC'),
      updated_at: Time.parse('2015-02-11 04: 31: 34 UTC'),
    }
  end

  describe '.import_updates' do
    context 'when there is no user' do
      it 'creates given user' do
        expect {
          User.import_updates(mojombo)
        }.to change {
          User.count
        }.from(0).to(1)

        user = User.last
        %i[id login avatar_url public_repos type].each do |attr|
          expect(user[attr]).to eq(mojombo[attr])
        end
      end
    end

    context 'when there is a given user' do
      let!(:user) { FactoryGirl.create(:user, old_attributes) }
      let(:old_attributes) do
        {
          id: mojombo[:id],
          login: 'k0kubun',
          avatar_url: 'google.com',
          public_repos: 0,
          type: 'Organization',
        }
      end
      let(:attributes) { %i[login avatar_url public_repos type] }

      it 'does not create an additional user' do
        expect {
          User.import_updates(mojombo)
        }.to_not change {
          User.count
        }.from(1)
      end

      it 'updates attributes' do
        expect {
          User.import_updates(mojombo)
        }.to change {
          user.reload
          attributes.map { |attr| user[attr] }
        }.from(attributes.map { |a| old_attributes[a]}).to(attributes.map { |a| mojombo[a] })
      end
    end

    context 'given existing user and absent user' do
      let!(:user) { FactoryGirl.create(:user, id: mojombo[:id]) }

      it 'creates absent user' do
        expect {
          User.import_updates([mojombo, defunkt])
        }.to change {
          User.count
        }.from(1).to(2)
      end
    end
  end
end
