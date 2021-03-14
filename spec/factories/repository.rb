FactoryBot.define do
  factory :repository do
    sequence(:id) { |n| n + 1 }
    name { 'gitstar-ranking' }
    full_name { 'k0kubun/gitstar-ranking' }
    association :owner, factory: :user
    description { 'GitHub Ranks' }
    fork { false }
    homepage { 'http://githubranking.com' }
    stargazers_count { 11 }
    language { 'Ruby' }
    fetched_at { nil }
    rank { 131197 }
  end
end
