FROM k0kubun/ruby:2.6.1
ENV RUBYOPT="--jit"

RUN cd /tmp && rm -f pubkey.gpg && wget https://dl.yarnpkg.com/debian/pubkey.gpg && apt-key add pubkey.gpg && \
  rm pubkey.gpg && echo "deb https://dl.yarnpkg.com/debian/ stable main" > /etc/apt/sources.list.d/yarn.list
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
  libmysqlclient-dev \
  nodejs \
  tzdata \
  yarn

RUN mkdir /app
WORKDIR /app

COPY Gemfile /app
COPY Gemfile.lock /app
RUN bundle install -j$(nproc) --without development test

COPY . /app
RUN bundle exec rake assets:precompile RAILS_ENV=production

CMD ["bundle", "exec", "puma", "-e", "production"]
