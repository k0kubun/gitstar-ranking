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
      rank:       0,
    )
    # TokenRegisterJob.perform_later(id, token)

    sign_in @user
    redirect_to @user
  end
end
