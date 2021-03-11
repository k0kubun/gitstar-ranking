class UpdateUserJob < ApplicationRecord
  # Kick UpdateUserWorker.java via MySQL queue
  # @param [Integer,nil] user_id
  # @param [String,nil] user_name
  # @param [Integer] token_user_id
  # @param [Integer,nil] wait
  def self.perform_later(user_name: nil, user_id: nil, token_user_id:, wait: nil)
    if user_name.nil? && user_id.nil?
      raise ArgumentError.new('user_name or user_id must be specified')
    end

    timeout_at = Time.now
    if wait
      timeout_at += wait
    end

    # Payload is parsed in: worker/src/main/java/com/github/k0kubun/gitstar_ranking/dao/UpdateUserJobDao.java
    payload = {
      user_id: user_id,
      user_name: user_name,
      token_user_id: token_user_id,
    }.compact
    create(payload: payload.to_json, timeout_at: timeout_at)
  end
end
