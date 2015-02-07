shared_context 'with sunspot stubbed', stub_sunspot: true do
  around do |example|
    Sunspot.session = Sunspot::Rails::StubSessionProxy.new(Sunspot.session)
    example.run
    Sunspot.session = Sunspot.session.original_session
  end
end
