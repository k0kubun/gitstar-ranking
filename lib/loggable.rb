module Loggable
  private

  def logger
    @logger ||= Logger.new(log_filename)
  end

  def log_filename
    klass_name = File.basename(self.class.to_s.underscore)
    "log/#{klass_name}.log"
  end
end
