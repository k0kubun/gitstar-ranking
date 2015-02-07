shared_context 'with frozen time' do |options|
  let(:frozen_time) { options[:at] } if options && options[:at]

  around do |example|
    Timecop.freeze(frozen_time)
    example.run
    Timecop.return
  end
end
