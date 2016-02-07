require 'disc'

Dir['./jobs/**/*.rb'].each do |job|
  require job
end
