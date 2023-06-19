FROM openjdk:11
ARG SITMUN_VERSION

WORKDIR /heroku-dev
    
RUN git clone -b master https://github.com/sitmun/sitmun-backend-core.git
WORKDIR /heroku-dev/sitmun-backend-core/
RUN ./gradlew clean :deploy:heroku-dev-lite:build -x test
RUN mv deploy/heroku-dev-lite/build/libs/heroku-dev-lite-$SITMUN_VERSION.jar /heroku-dev/sitmun.jar

ENTRYPOINT ["java", "-jar", "/heroku-dev/sitmun.jar"]
