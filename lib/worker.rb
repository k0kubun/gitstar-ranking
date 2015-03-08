require 'thread'

class Worker
  POISON = Object.new

  class WrappedException < StandardError
    attr_reader :exception
    def initialize(exception)
      @exception = exception
    end
  end

  def initialize(size, func)
    @request_queue  = Queue.new
    @response_queue = Queue.new
    @func = func
    @threads = size.times.map { |i| Thread.start { process_queue(i) } }
    trap("INT") { abort_threads }
  end

  def enq(object)
    @request_queue.enq(object)
  end

  def deq
    result = @response_queue.deq
    raise result.exception if result.is_a?(WrappedException)
    result
  end

  def stop
    stop_threads
  end

  private

  def process_queue(i)
    loop do
      object = @request_queue.deq
      break if object.equal? POISON
      @response_queue.enq apply_func(object, i)
    end
  end

  def apply_func(object, i)
    @func.call(object, i)
  rescue Exception => e
    WrappedException.new(e)
  end

  def stop_threads
    @threads.each { @request_queue.enq POISON }
    @threads.each { |thread| thread.join }
  end

  def abort_threads
    @threads.each { |i| i.exit }
    exit 1
  end
end
