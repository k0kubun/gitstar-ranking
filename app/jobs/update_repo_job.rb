class UpdateRepoJob < ActiveJob::Base
  queue_as :default

  def perform(user_id)
    # pending. old import job is removed and obsolete.
  end
end
