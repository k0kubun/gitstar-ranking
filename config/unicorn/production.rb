require 'dotenv'
Dotenv.load

# paths
if ENV.key?('DEPLOY_TO')
  app_path = ENV['DEPLOY_TO']
  working_directory "#{app_path}/current"
  pid               "#{app_path}/current/tmp/pids/unicorn.pid"

  # use correct Gemfile on restarts
  before_exec do |server|
    # http://eagletmt.hateblo.jp/entry/2015/02/21/015956
    Dotenv.overload

    ENV['BUNDLE_GEMFILE'] = "#{app_path}/current/Gemfile"
  end
end

# listen
if ENV.key?('UNICORN_SOCKET_PATH')
  listen ENV['UNICORN_SOCKET_PATH']
else
  listen Integer(ENV.fetch('UNICORN_PORT'))
end

# logging
if ENV["RAILS_LOG_TO_STDOUT"] != 'true'
  stderr_path "log/unicorn.stderr.log"
  stdout_path "log/unicorn.stdout.log"
end

# workers
worker_processes Integer(ENV.fetch('UNICORN_NUM_WORKERS', 3))

# preload
preload_app true

before_fork do |server, worker|
  # the following is highly recomended for Rails + "preload_app true"
  # as there's no need for the master process to hold a connection
  if defined?(ActiveRecord::Base)
    ActiveRecord::Base.connection.disconnect!
  end

  # Before forking, kill the master process that belongs to the .oldbin PID.
  # This enables 0 downtime deploys.
  old_pid = "#{server.config[:pid]}.oldbin"
  if File.exists?(old_pid) && server.pid != old_pid
    begin
      Process.kill("QUIT", File.read(old_pid).to_i)
    rescue Errno::ENOENT, Errno::ESRCH
      # someone else did our job for us
    end
  end
end

after_fork do |server, worker|
  if defined?(ActiveRecord::Base)
    ActiveRecord::Base.establish_connection
  end
end
