FROM openjdk:11
ARG PROXY_VERSION
RUN apt-get update -y \
    && apt-get upgrade -y \
    && apt-get install git -y

WORKDIR /proxy-middleware
    
RUN git clone -b main https://github.com/sitmun/sitmun-proxy-middleware.git

WORKDIR sitmun-proxy-middleware

RUN ./gradlew clean build -x test

RUN mv /proxy-middleware/sitmun-proxy-middleware/build/libs/sitmun-proxy-middleware-${PROXY_VERSION}.jar /proxy-middleware/proxy-middleware.jar

ENTRYPOINT ["java", "-jar", "/proxy-middleware/proxy-middleware.jar"]
