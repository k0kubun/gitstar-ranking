desc 'Sweep failed records'
task sweep: :environment do
  FailureSweeper.new.run
end
