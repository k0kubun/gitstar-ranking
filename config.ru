# This file is used by Rack-based servers to start the application.

require ::File.expand_path('../config/environment', __FILE__)
require 'unicorn/oob_gc'

use Unicorn::OobGC, 10 if Rails.env.production?
run Rails.application
