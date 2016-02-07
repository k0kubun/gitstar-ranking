class UpdateUser
  include Disc::Job
  disc queue: 'default'

  def perform(user_id)
    puts user_id
  end
end
