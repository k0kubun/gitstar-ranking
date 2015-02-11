module Benchmarkable
  private

  def benchmark(name, &block)
    start = Time.now
    result = block.call
    diff = Time.now - start
    puts "#{name} (#{diff}s)"

    result
  end
end
