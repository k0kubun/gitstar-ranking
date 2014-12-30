class OmniauthCallbacksController < Devise::OmniauthCallbacksController
  def github
    auth = request.env['omniauth.auth']
    uid  = auth && auth[:uid]

    @user = User.find(uid)
    sign_in @user
    redirect_to @user
  end
end
