[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sitmun%3Asitmun-proxy-middleware&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sitmun%3Asitmun-proxy-middleware)

# SITMUN proxy middleware

The **SITMUN Proxy Middleware** is a reverse proxy and middleware that facilitates the access of the **SITMUN Map Viewer** to protected services and databases.

These protected services or databases can have various restrictions or requirements, such as:

- They are located on an Intranet, and users outside the Intranet cannot access them directly.
- They require access credentials that should not be disclosed to the users.
- User requests must be modified and validated before being forwarded to the protected service.
- The service’s response needs to be modified before returning it to the client application (e.g., masking part of an image with a map).

## Prerequisites

Before you begin, ensure you have met the following requirements:

- You have a `Windows/Linux/Mac` machine.
- You have installed the latest version of [Docker CE](https://docs.docker.com/engine/install/) and [Docker Compose](https://docs.docker.com/compose/install/), or [Docker Desktop](https://www.docker.com/products/docker-desktop/).
  Docker CE is fully open-source, while Docker Desktop is a commercial product.
- You have installed [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) on your machine.
- You have a basic understanding of Docker, Docker Compose, and Git.
- You have internet access on your machine to pull Docker images and Git repositories.
- You have a running instance of `[sitmun-backend-core](https://github.com/sitmun/sitmun-backend-core)` to make requests (e.g. `http://localhost:9001/api/config/proxy`).
- You know the key to access such instance `SITMUN_BACKEND_CONFIG_SECRET` (e.g. `abcd`)

## Installing SITMUN Proxy Middleware

To install the SITMUN Proxy Middleware, follow these steps:

1. Clone the repository:

    ```bash
    git clone https://github.com/sitmun/sitmun-proxy-middleware.git
    ```

2. Change to the directory of the repository:

    ```bash
    cd sitmun-proxy-middleware
    ```

3. Create a new file named `.env` inside the directory.
   Open the `.env` file in a text editor and add in the following format:

    ```properties
    SITMUN_BACKEND_CONFIG_URL=the_location_of_the_sitmun_backend_configuration_endpoint
    SITMUN_BACKEND_CONFIG_SECRET=the_shared_secret
    ```

4. Start the SITMUN Middleware proxy:

    ```bash
    docker compose up
    ```

   This command will build and start all the services defined in the `docker-compose.yml` file.

5. Access the SITMUN Middleware Proxy at [http://localhost:9002/actuator/health](http://localhost:9002/actuator/health) and expect:

   ```json
   {"status":"UP"}
   ```

See [SITMUN Application Stack](https://github.com/sitmun/sitmun-application-stack) as an example of how to deploy and run the proxy as parte of the SITMUN stack.

## Configuration

The following environment variables are required:

- `SITMUN_BACKEND_CONFIG_URL`: The URL to the backend service that provides the configuration for the proxy.
- `SITMUN_BACKEND_CONFIG_SECRET`: The secret key to access the configuration service. It must be the same as the one used in the backend service.

Additional information is available at <https://sitmun.github.io/architecture/>.

## Uninstalling SITMUN Proxy middleware

To stop and remove all services, volumes, and networks defined in the `docker-compose.yml` file, use:

```bash
docker compose down -v
```

## Contributing to SITMUN Application Stack

To contribute to SITMUN Application Stack, follow these steps:

1. **Fork this repository** on GitHub.
2. **Clone your forked repository** to your local machine.
3. **Create a new branch** for your changes.
4. **Make your changes** and commit them.
5. **Push your changes** to your forked repository.
6. **Create the pull request** from your branch on GitHub.

Alternatively, see the GitHub documentation on [creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).

## License

This project uses the following license: [European Union Public Licence V. 1.2](LICENSE).
