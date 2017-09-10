class OmniauthCallbacksController < Devise::OmniauthCallbacksController
  def github
    auth = request.env['omniauth.auth']
    raise BadRequest if auth.blank?

    id    = auth[:uid]
    login = auth[:info][:nickname]
    image = auth[:info][:image]
    token = auth[:credentials][:token]

    @user = User.where(id: id).first_or_create(
      login:      login,
      type:       'User',
      avatar_url: image,
    )
    register_token(id, token)

    sign_in @user
    redirect_to @user
  end

  private

  def register_token(user_id, token)
    access_token = AccessToken.where(user_id: user_id).first_or_create(token: token)
    access_token.update(token: token) if access_token.token != token
  end
end
