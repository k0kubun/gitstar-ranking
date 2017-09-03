FactoryGirl.define do
  factory :repository do
    name 'githubranking'
    full_name 'k0kubun/githubranking'
    association :owner, factory: :user
    description 'GitHub Ranks'
    fork false
    homepage 'http://githubranking.com'
    stargazers_count 11
    language 'Ruby'
    fetched_at nil
    rank 131197
  end
end
