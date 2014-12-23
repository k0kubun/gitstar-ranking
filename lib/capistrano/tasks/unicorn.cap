namespace :unicorn do
  task :environment do
    set :unicorn_pid, "#{shared_path}/tmp/pids/unicorn.pid"
    set :unicorn_config, "#{current_path}/config/unicorn/#{fetch(:rails_env)}.rb"
  end

  def start_unicorn
    within current_path do
      execute :bundle, :exec, :unicorn, "-c #{fetch(:unicorn_config)} -E #{fetch(:rails_env)} -D"
    end
  end

  def stop_unicorn
    execute :kill, "-s QUIT $(< #{fetch(:unicorn_pid)})"
  end

  def reload_unicorn
    execute :kill, "-s USR2 $(< #{fetch(:unicorn_pid)})"
  end

  def force_stop_unicorn
    execute :kill, "$(< #{fetch(:unicorn_pid)})"
  end

  desc "Start unicorn server"
  task :start => :environment do
    on roles(:app) do
      start_unicorn
    end
  end

  desc "Stop unicorn server gracefully"
  task :stop => :environment do
    on roles(:app) do
      stop_unicorn
    end
  end

  desc "Restart unicorn server gracefully"
  task :restart => :environment do
    on roles(:app) do
      if test("[ -f #{fetch(:unicorn_pid)} ]")
        reload_unicorn
      else
        start_unicorn
      end
    end
  end

  desc "Stop unicorn server immediately"
  task :force_stop => :environment do
    on roles(:app) do
      force_stop_unicorn
    end
  end
end
