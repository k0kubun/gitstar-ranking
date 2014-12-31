class OmniauthCallbacksController < Devise::OmniauthCallbacksController
  def github
    auth = request.env['omniauth.auth']
    raise BadRequest if auth.blank?

    id    = auth[:uid]
    login = auth[:info][:nickname]
    image = auth[:info][:image]

    @user = User.where(id: id).first_or_create(
      login:      login,
      type:       'User',
      avatar_url: image,
      rank:       last_rank,
    )
    sign_in @user
    redirect_to @user
  end

  private

  def last_rank
    User.order(rank: :desc).select(:rank).first.rank
  end
end
