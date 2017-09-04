Raven.configure do |config|
  config.excluded_exceptions += [
    ActionController::BadRequest,
  ].map(&:name)
end
