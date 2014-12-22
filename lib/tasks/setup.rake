desc 'Create copy for local development'
task :setup do
  TARGET = {
    'config/secrets.yml.sample' => 'config/secrets.yml',
  }

  TARGET.each do |from, to|
    `cp #{from} #{to}`
  end
end
