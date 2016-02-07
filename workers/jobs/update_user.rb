class UpdateUser
  include Disc::Job
  disc queue: 'default'

  def perform(user_id)
    $stdout.puts 'stdout', user_id
    $stderr.puts 'stderr', user_id
  end
end
