ENV['RAILS_ENV'] ||= 'test'
require 'spec_helper'
require File.expand_path('../../config/environment', __FILE__)
require 'rspec/rails'
# Add additional requires below this line. Rails is not loaded until this point!

RSpec.configure do |config|
  config.fixture_path = Rails.root.join('spec/fixtures').to_s

  config.use_transactional_fixtures = true

  config.infer_spec_type_from_file_location!
end

Dir[Rails.root.join("spec/factories/**/*.rb")].each { |f| require f }

Dir[Rails.root.join("spec/support/**/*.rb")].each { |f| require f }
