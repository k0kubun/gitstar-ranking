FactoryGirl.define do
  factory :user do
    login 'k0kubun'
    avatar_url 'https://avatars.githubusercontent.com/u/3138447?v=3'
    type 'User'
    site_admin false
    queued_at Time.now
    stargazers_count 195
    rank 11974
  end
end
