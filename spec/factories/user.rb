FactoryBot.define do
  factory :user do
    sequence(:login) { |n| "user_#{n}" }
    avatar_url { 'https://avatars.githubusercontent.com/u/3138447?v=3' }
    type { 'User' }
    queued_at { Time.now }
    stargazers_count { 195 }
    rank { 11974 }
  end
end
