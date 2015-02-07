require 'rails_helper'

describe UserFetchJob do
  describe '#perform', stub_sunspot: true do
    let!(:job) { described_class.new }
    let(:user) { FactoryGirl.create(:user) }
    let(:old_name) { 'activerecord-count_loader' }
    let(:new_name) { 'activerecord-precount' }
    let(:old_star) { 5 }
    let(:new_star) { 15 }
    let(:old_full_name) { "#{user.login}/#{old_name}" }
    let(:new_full_name) { "#{user.login}/#{new_name}" }
    let(:repository) { nil }
    let(:all_repos) do
      [{
        id: repository.try(:id) || 23962928,
        name: new_name,
        full_name: "#{user.login}/#{new_name}",
        owner: {
          login: 'k0kubun',
          id: user.id,
          avatar_url: 'https://avatars.githubusercontent.com/u/3138447?v=3',
          gravatar_id: '',
          url: 'https://api.github.com/users/k0kubun',
          html_url: 'https://github.com/k0kubun',
          followers_url: 'https://api.github.com/users/k0kubun/followers',
          following_url: 'https://api.github.com/users/k0kubun/following{/other_user}',
          gists_url: 'https://api.github.com/users/k0kubun/gists{/gist_id}',
          starred_url: 'https://api.github.com/users/k0kubun/starred{/owner}{/repo}',
          subscriptions_url: 'https://api.github.com/users/k0kubun/subscriptions',
          organizations_url: 'https://api.github.com/users/k0kubun/orgs',
          repos_url: 'https://api.github.com/users/k0kubun/repos',
          events_url: 'https://api.github.com/users/k0kubun/events{/privacy}',
          received_events_url: 'https://api.github.com/users/k0kubun/received_events',
          type: 'User',
          site_admin: false,
        },
        private: false,
        html_url: 'https://github.com/k0kubun/activerecord-precount',
        description: 'N+1 count query killer for ActiveRecord',
        fork: false, url: 'https://api.github.com/repos/k0kubun/activerecord-precount',
        forks_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/forks',
        keys_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/keys{/key_id}',
        collaborators_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/collaborators{/collaborator}',
        teams_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/teams',
        hooks_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/hooks',
        issue_events_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/issues/events{/number}',
        events_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/events',
        assignees_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/assignees{/user}',
        branches_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/branches{/branch}',
        tags_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/tags',
        blobs_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/git/blobs{/sha}',
        git_tags_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/git/tags{/sha}',
        git_refs_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/git/refs{/sha}',
        trees_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/git/trees{/sha}',
        statuses_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/statuses/{sha}',
        languages_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/languages',
        stargazers_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/stargazers',
        contributors_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/contributors',
        subscribers_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/subscribers',
        subscription_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/subscription',
        commits_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/commits{/sha}',
        git_commits_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/git/commits{/sha}',
        comments_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/comments{/number}',
        issue_comment_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/issues/comments/{number}',
        contents_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/contents/{+path}',
        compare_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/compare/{base}...{head}',
        merges_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/merges',
        archive_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/{archive_format}{/ref}',
        downloads_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/downloads',
        issues_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/issues{/number}',
        pulls_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/pulls{/number}',
        milestones_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/milestones{/number}',
        notifications_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/notifications{?since,all,participating}',
        labels_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/labels{/name}',
        releases_url: 'https://api.github.com/repos/k0kubun/activerecord-precount/releases{/id}',
        created_at: Time.parse('2014-09-12 14:04:27 UTC'),
        updated_at: Time.parse('2015-02-02 18:58:06 UTC'),
        pushed_at: Time.parse('2015-02-01 08:09:50 UTC'),
        git_url: 'git: //github.com/k0kubun/activerecord-precount.git',
        ssh_url: 'git@github.com: k0kubun/activerecord-precount.git',
        clone_url: 'https://github.com/k0kubun/activerecord-precount.git',
        svn_url: 'https://github.com/k0kubun/activerecord-precount',
        homepage: nil,
        size: 886,
        stargazers_count: new_star,
        watchers_count: new_star,
        language: 'Ruby',
        has_issues: true,
        has_downloads: true,
        has_wiki: true,
        has_pages: false,
        forks_count: 1,
        mirror_url: nil,
        open_issues_count: 2,
        forks: 1,
        open_issues: 2,
        watchers: 15,
        default_branch: 'master',
        permissions: {
          admin: true,
          push: true,
          pull: true,
        },
      }]
    end

    before do
      allow(job).to receive(:all_repos).with(user.id).and_return(all_repos)
    end

    context 'when there is no repo' do
      it 'creates fetched repos' do
        expect {
          job.perform(user.id)
        }.to change {
          Repository.count
        }.from(0).to(1)

        repo = Repository.last
        expect(repo.name).to eq(new_name)
        expect(repo.stargazers_count).to eq(new_star)
      end
    end

    context 'when there is an existing repo with old name' do
      let!(:repository) do
        FactoryGirl.create(
          :repository,
          name: old_name,
          full_name: old_full_name,
          stargazers_count: old_star,
        )
      end

      it 'updates fetched repos' do
        expect {
          job.perform(user.id)
        }.to change {
          repository.reload
          [repository.name, repository.full_name, repository.stargazers_count]
        }.from([old_name, old_full_name, old_star]).to([new_name, new_full_name, new_star])
      end
    end

    context 'when there is a deleted repo' do
      include_context 'with frozen time', at: Time.local(2015, 2, 8, 1, 51)

      let!(:deleted_repository) { FactoryGirl.create(:repository, owner: user) }

      it 'destroys an obsolete repo' do
        expect {
          job.perform(user.id)
        }.to_not change {
          Repository.count
        }.from(1)

        expect(Repository.last.id).to_not eq(deleted_repository.id)
        expect(user.reload.queued_at).to eq(Time.now)
      end
    end

    context 'when user is deleted on GitHub and API responds with NotFound' do
      let!(:repository) { FactoryGirl.create(:repository, owner: user) }

      before do
        allow(job).to receive(:all_repos).with(user.id).and_raise(Octokit::NotFound)
      end

      it 'destroys given user and his repositories' do
        expect {
          job.perform(user.id)
        }.to change {
          [User.count, Repository.count]
        }.from([1, 1]).to([0, 0])
      end
    end
  end
end
