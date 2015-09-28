# This file is used by Rack-based servers to start the application.

require ::File.expand_path('../config/environment', __FILE__)
require 'gctools/oobgc'

use GC::OOB::UnicornMiddleware if Rails.env.production?
run Rails.application
