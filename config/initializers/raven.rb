Raven.configure do |config|
  config.excluded_exceptions += [
    ActionController::BadRequest,
    ActionController::UnknownFormat,
  ].map(&:name)
end
