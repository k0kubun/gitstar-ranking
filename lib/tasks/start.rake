desc 'Start applications via foreman without rails, for binding.pry-ability in rails s'
task :start do
  apps    = []
  process = {}

  File.read('Procfile').each_line do |line|
    app = line.gsub(/:.+$/, '').strip
    if app == 'rails'
      process[app] = 0
    else
      process[app] = 1
    end
  end

  process_arg = process.map { |p| p.join('=') }.join(',')
  system("bundle exec foreman start -m '#{process_arg}'")
end
