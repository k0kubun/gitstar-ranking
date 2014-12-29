Rails.application.config.middleware.use OmniAuth::Builder do
  provider :github, ENV['OMNIAUTH_CONSUMER_KEY'], ENV['OMNIAUTH_CONSUMER_SECRET']
end
