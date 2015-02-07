FactoryGirl.define do
  factory :organization do
    login 'cookpad'
    avatar_url 'https//avatars.githubusercontent.com/u/119195?v=3'
    type 'Organization'
    site_admin false
    queued_at Time.now
    stargazers_count 2405
    rank 588
  end
end
