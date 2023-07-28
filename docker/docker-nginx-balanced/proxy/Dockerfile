FROM openjdk:11
ARG PROXY_VERSION

WORKDIR /proxy-middleware
    
RUN git clone -b main https://github.com/sitmun/sitmun-proxy-middleware.git

WORKDIR /proxy-middleware/sitmun-proxy-middleware
RUN ./gradlew clean build -x test
RUN mv build/libs/sitmun-proxy-middleware-$PROXY_VERSION.jar /proxy-middleware/proxy-middleware.jar


ENTRYPOINT ["java", "-jar", "/proxy-middleware/proxy-middleware.jar"]
