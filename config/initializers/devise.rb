Devise.setup do |config|
  # The secret key used by Devise.
  config.secret_key = ENV['DEVISE_SECRET_KEY'] || '612d5303dbeca2425f891abdc4ba08c7f5e70131e3f887c134a8afd1d34e6a5f9b4dd013c71557437e48a8f1c76761ab8bd52576c819b04d329afac7fa26dff8'

  # ==> Mailer Configuration
  config.mailer_sender = 'devise@githubranking.com'

  # ==> ORM configuration
  require 'devise/orm/active_record'

  # ==> Configuration for any authentication mechanism
  config.authentication_keys = [:id]

  # You can skip storage for particular strategies by setting this option.
  config.skip_session_storage = [:http_auth]

  # ==> Configuration for :database_authenticatable
  config.stretches = Rails.env.test? ? 1 : 10

  # If true, requires any email changes to be confirmed (exactly the same way as
  # initial account confirmation) to be applied.
  config.reconfirmable = true

  # Invalidates all the remember me tokens when the user signs out.
  config.expire_all_remember_me_on_sign_out = true

  # ==> Configuration for :validatable
  config.password_length = 8..128

  # Time interval you can reset your password with a reset password key.
  config.reset_password_within = 6.hours

  # The default HTTP method used to sign out a resource. Default is :delete.
  config.sign_out_via = :delete

  # ==> OmniAuth
  if Rails.env.development?
    config.omniauth :github, '4c2d6f040d55517738ed', 'edf1307cf3f6120a2af69010ff5a7faa2b02d977'
  else
    config.omniauth :github, ENV['OMNIAUTH_CONSUMER_KEY'], ENV['OMNIAUTH_CONSUMER_SECRET']
  end
end
