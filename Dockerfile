FROM k0kubun/ruby:2.6.0-preview2
ENV RUBYOPT="--jit"

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
  libmysqlclient-dev \
  nodejs \
  tzdata

RUN mkdir /app
WORKDIR /app

COPY Gemfile /app
COPY Gemfile.lock /app
RUN bundle install -j$(nproc) --without development test

COPY . /app

CMD ["bundle", "exec", "unicorn", "-c", "config/unicorn/production.rb", "-E", "production"]
