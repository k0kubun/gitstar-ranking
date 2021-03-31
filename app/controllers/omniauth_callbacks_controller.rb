class OmniauthCallbacksController < Devise::OmniauthCallbacksController
  def github
    auth = request.env['omniauth.auth']
    raise BadRequest if auth.blank?

    id    = auth[:uid]&.to_i
    login = auth[:info][:nickname]
    image = auth[:info][:image]
    token = auth[:credentials][:token]

    @user = without_user_conflict(id: id, login: login) do 
      User.where(id: id).first_or_create(
        login:      login,
        type:       'User',
        avatar_url: image,
      )
    end
    register_token(id, token)

    sign_in @user
    redirect_to @user
  end

  private

  def without_user_conflict(id:, login:, &block)
    # Delete a conflicting user who changed its login
    user = User.find_by(login: login)
    if user && user.id != id
      user.destroy
    end

    result = block.call

    if user && user.id != id
      UserUpdateJob.perform_later(user_id: user.id, token_user_id: id)
    end
    result
  end

  def register_token(user_id, token)
    access_token = AccessToken.where(user_id: user_id).first_or_create(token: token)
    access_token.update(token: token) if access_token.token != token
  end
end
