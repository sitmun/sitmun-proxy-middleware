FROM openjdk:11
ARG SITMUN_VERSION
RUN apt-get update -y \
    && apt-get upgrade -y \
    && apt-get install git -y

WORKDIR /heroku-dev
    
RUN git clone -b master https://github.com/sitmun/sitmun-backend-core.git

WORKDIR sitmun-backend-core

RUN ./gradlew clean build -x test

RUN mv /heroku-dev/sitmun-backend-core/deploy/heroku-dev-lite/build/libs/heroku-dev-lite-${SITMUN_VERSION}.jar /heroku-dev/sitmun.jar

ENTRYPOINT ["java", "-jar", "/heroku-dev/sitmun.jar"]
