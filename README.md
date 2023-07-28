[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sitmun%3Asitmun-proxy-middleware&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sitmun%3Asitmun-proxy-middleware)

# SITMUN proxy middleware

Middleware proxy: is a reverse proxy to allow access to protected services and databases (either because they are on an
Intranet, and users are outside it, or because they require access credentials and users should not know them, or
because they want to modify the information they provide before returning it to the client application, e.g. masking
part of an image with a map).

It is a reverse proxy and therefore the client cannot choose whether to use it or not, and it is essentially transparent
to the client.

## Requirements

`sitmun-proxy-middleware` depends on `sitmun-backend-core` to make requests to the backend that return the
configurations needed to make requests to the end services.

## Deployments

The **SITMUN proxy middleware** can be deployed using docker:

Because `sitmun-proxy-middleware` depends on `sitmun-backend-core`.
It is needed to run a docker compose with backend image and requires a previous build for `sitmun-backend-core`.

The docker-compose.yml is located in the folder `docker` and can run executing in the folder the next command in a
terminal:

```bash
docker compose up
```

The proxy image build download the sitmun-proxy-middleware repository and run a gradle build.

**SITMUN proxy middleware** can also be deployed locally via a jar file.
To get the jar, we have to compile the project with the following command:

```bash
gradlew clean build
```

This creates a jar that we can deploy for local testing:

```bash
java -jar sitmun-proxy-middleware-${version}.jar
```

Regardless of the deployment method used, the proxy depends on the `sitmun-backend-core` as it makes use of the
configuration and authorization api.

The API definitions for this deployment can be found
at https://sitmun-backend-core.herokuapp.com/swagger-ui/index.html?urls.primaryName=API%20del%20Proxy.

## Developer documentation

Additional information is available at https://sitmun.github.io/arquitectura/Arq_SITMUN.html
