class UpdateUserJob < ApplicationRecord
  # Kick UpdateUserWorker.java via MySQL queue
  # @param [Integer] user_id
  # @param [String] user_name
  # @param [Integer] token_user_id
  def self.perform_later(user_name: nil, user_id: nil, token_user_id:)
    if user_name.nil? && user_id.nil?
      raise ArgumentError.new('user_name or user_id must be specified')
    end

    # Payload is parsed in: worker/src/main/java/com/github/k0kubun/github_ranking/dao/UpdateUserJobDao.java
    payload = {
      user_id: user_id&.to_s, # to avoid using `javax.json.JsonObject#getInt` for precision in worker side
      user_name: user_name,
      token_user_id: token_user_id.to_s, # to avoid using `javax.json.JsonObject#getInt` for precision in worker side
    }.compact
    create(payload: payload.to_json, timeout_at: Time.now)
  end
end
