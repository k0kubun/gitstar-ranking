FROM azul/zulu-openjdk:8u162

COPY gradle /tmp/gradle
COPY gradlew build.gradle settings.gradle /tmp/
RUN cd /tmp && ./gradlew buildNeeded

COPY . /app
WORKDIR /app
RUN ./gradlew installDist

CMD ["build/install/worker/bin/worker"]
