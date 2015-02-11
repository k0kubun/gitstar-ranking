# To interrupt batch excecution safely, execute `touch /tmp/***`.
module Interruptable
  private

  def accept_interrupt
    if File.exists?(interrupter_filename)
      puts 'Detected interrupter file'
      remove_interrupter
      exit 0
    end
  end

  def remove_interrupter
    File.rm_f(interrupter_filename)
  end

  def interrupter_filename
    klass_name = File.basename(self.class.to_s.underscore)
    "/tmp/#{klass_name}"
  end
end
