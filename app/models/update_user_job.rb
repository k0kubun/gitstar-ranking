class UpdateUserJob < ActiveRecord::Base
  # Kick UpdateUserWorker.java via MySQL queue
  # @param [Integer] user_id
  # @param [Integer] token_user_id
  def self.perform_later(user_id:, token_user_id:)
    # Payload is parsed in: worker/src/main/java/com/github/k0kubun/github_ranking/dao/UpdateUserJobDao.java
    payload = {
      user_id: user_id,
      token_user_id: token_user_id,
    }
    create(payload: payload.to_json, timeout_at: Time.now)
  end
end
