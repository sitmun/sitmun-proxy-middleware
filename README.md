[![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL%20v1.2-blue.svg)](LICENSE)
![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)

# SITMUN Proxy Middleware

A Spring Boot microservice that acts as a reverse proxy and middleware to facilitate secure access to protected services and databases for the SITMUN Map Viewer. This service is part of the [SITMUN](https://sitmun.github.io/) geospatial platform ecosystem.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Docker Deployment](#docker-deployment)
  - [Troubleshooting](#troubleshooting)
  - [Building](#building)
  - [Packaging Options](#packaging-options)
- [Features](#features)
  - [Core Functionality](#core-functionality)
  - [Security Features](#security-features)
  - [Development Features](#development-features)
- [API Reference](#api-reference)
  - [Endpoints](#endpoints)
  - [Usage Examples](#usage-examples)
  - [Request Parameters](#request-parameters)
- [Configuration](#configuration)
  - [Environment Variables](#environment-variables)
  - [Profiles](#profiles)
  - [Configuration Files](#configuration-files)
- [Architecture](#architecture)
  - [Technology Stack](#technology-stack)
  - [System Architecture](#system-architecture)
  - [Key Components](#key-components)
  - [Request Processing Flow](#request-processing-flow)
  - [Decorator Pattern](#decorator-pattern)
  - [Extensibility](#extensibility)
  - [Error Handling](#error-handling)
- [Development](#development)
  - [Profiles Explained](#profiles-explained)
  - [Deployment](#deployment)
  - [Project Structure](#project-structure)
  - [Build System](#build-system)
  - [Code Quality](#code-quality)
  - [Running Quality Checks](#running-quality-checks)
  - [Version Management](#version-management)
  - [Testing](#testing)
  - [Development Workflow](#development-workflow)
- [Advanced Features](#advanced-features)
  - [Security and Authentication](#security-and-authentication)
  - [Monitoring and Observability](#monitoring-and-observability)
- [Contributing](#contributing)
  - [Development Guidelines](#development-guidelines)
- [Integration with SITMUN](#integration-with-sitmun)
  - [Prerequisites](#prerequisites-1)
  - [Configuration Steps](#configuration-steps)
  - [Service Types and Configuration](#service-types-and-configuration)
  - [Security Configuration](#security-configuration)
  - [Monitoring and Health Checks](#monitoring-and-health-checks)
  - [Troubleshooting Integration](#troubleshooting-integration)
  - [SITMUN Application Stack](#sitmun-application-stack)
- [Support](#support)
- [License](#license)

## Overview

The SITMUN Proxy Middleware provides secure proxy functionality to:

- **Access Protected Services**: Bridge connections to services located on intranets or requiring special access
- **Credential Management**: Handle authentication without exposing credentials to end users
- **Request Modification**: Transform and validate requests before forwarding to protected services
- **Response Processing**: Modify service responses before returning to client applications
- **Security Layer**: Provide an additional security layer for sensitive geospatial services

This service integrates with the [SITMUN Backend Core](https://github.com/sitmun/sitmun-backend-core) to provide secure proxy capabilities for the SITMUN platform.

## Quick Start

### Prerequisites

- Java 17 or later
- Docker CE or Docker Desktop
- Git
- Running instance of [sitmun-backend-core](https://github.com/sitmun/sitmun-backend-core)

### Local Development

1. **Clone the repository**

   ```bash
   git clone https://github.com/sitmun/sitmun-proxy-middleware.git
   cd sitmun-proxy-middleware
   ```

2. **Build the application**

   ```bash
   ./gradlew build -x test
   ```

3. **Run the application**

   ```bash
   # Run with Java directly (recommended)
   java -jar build/libs/sitmun-proxy-middleware.jar --spring.profiles.active=prod
   
   # Or use Gradle bootRun directly
   ./gradlew bootRun --args='--spring.profiles.active=prod'
   ```

4. **Verify the service is running**

   ```bash
   # Check health status
   curl http://localhost:8080/actuator/health
   
   # Test the proxy endpoint (will return 400 for invalid request, but confirms service is running)
   curl -X GET http://localhost:8080/proxy/1/1/test/1
   ```

### Docker Deployment

> **Note:** Docker builds use JAR packaging only. For WAR deployments, build locally and deploy to your application server.

1. **Create environment configuration**

   ```bash
   # Create .env file
   cat > .env << EOF
   SITMUN_BACKEND_CONFIG_URL=http://localhost:9001/api/config/proxy
   SITMUN_BACKEND_CONFIG_SECRET=your-secret-key
   EOF
   ```

2. **Start with Docker Compose**

   ```bash
   cd docker/development
   docker-compose up
   ```

3. **Verify deployment**

   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Troubleshooting

#### Port Already in Use

```bash
# Use different port
./gradlew bootRun --args='--spring.profiles.active=prod --server.port=8081'
```

#### Memory Issues

```bash
# Increase heap size
./gradlew bootRun --args='--spring.profiles.active=prod -Xmx4g -Xms2g'
```

#### Docker Issues

```bash
# Clean up Docker resources
cd docker/development
docker-compose down -v
docker system prune -f
```

### Building

```bash
# Build the project (includes Git hooks setup)
./gradlew build

# Build without tests (faster for development)
./gradlew build -x test

# Run tests
./gradlew test

# Create JAR file
./gradlew jar

# Format code
./gradlew spotlessApply

# Check code coverage
./gradlew jacocoTestReport
```

> **💡 Tip**: For development, use `./gradlew build -x test` for faster builds, then run the JAR directly with `java -jar build/libs/sitmun-proxy-middleware.jar --spring.profiles.active=dev`

### Packaging Options

The application supports two packaging formats:

#### JAR (Default)

Standalone executable JAR with embedded Tomcat server.

```bash
# Build JAR (default)
./gradlew build

# Or explicitly
./gradlew build -Ppackaging=jar

# Run JAR
java -jar build/libs/sitmun-proxy-middleware.jar --spring.profiles.active=prod
```

**Use JAR when:**

- Deploying with Docker (only JAR is supported)
- Running as a standalone microservice
- Using Spring Boot's embedded server

#### WAR

Web Application Archive for deployment to external servlet containers.

```bash
# Build WAR
./gradlew build -Ppackaging=war

# Output: build/libs/sitmun-proxy-middleware.war
```

**Use WAR when:**

- Deploying to existing Tomcat, WildFly, or WebSphere servers
- Required by organizational infrastructure policies
- Need to run multiple applications on the same servlet container

**Deployment Example (Tomcat):**

```bash
# Copy WAR to Tomcat
cp build/libs/sitmun-proxy-middleware.war /path/to/tomcat/webapps/

# Tomcat will auto-deploy at:
# http://localhost:8080/sitmun-proxy-middleware/

# Or rename to ROOT.war for root context:
cp build/libs/sitmun-proxy-middleware.war /path/to/tomcat/webapps/ROOT.war
# http://localhost:8080/
```

**Configuring Active Profile for WAR:**

Unlike JAR files, WAR files cannot use command-line arguments. Configure the active profile using one of these methods:

**Method 1: Environment Variable (Recommended)**

Set the environment variable in your servlet container:

```bash
# For Tomcat, add to setenv.sh (or setenv.bat on Windows)
export SPRING_PROFILES_ACTIVE=prod

# For systemd service
[Service]
Environment="SPRING_PROFILES_ACTIVE=prod"
```

**Method 2: System Property**

Add to your servlet container's startup script:

```bash
# For Tomcat, add to catalina.sh
export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=prod"
```

**Method 3: JNDI (Enterprise Deployments)**

For application servers like WildFly or WebSphere, configure via JNDI or server configuration.

**Method 4: application.properties in WAR**

You can also include a `WEB-INF/classes/application.properties` file in the WAR with:

```properties
spring.profiles.active=prod
```

**Note:** The `ServletInitializer` class enables WAR deployment by configuring the application for external servlet containers.

## Features

### Core Functionality

- **Reverse Proxy**: Route requests to protected services with authentication
- **Request Decorators**: Modify requests using configurable decorator patterns
- **Response Decorators**: Transform responses before returning to clients
- **Authentication Handling**: Manage credentials and tokens securely
- **Multi-Service Support**: Handle different types of services (HTTP, JDBC, etc.)
- **Dynamic Configuration**: Load proxy configuration from SITMUN backend
- **Request Validation**: Validate and sanitize incoming requests
- **Error Handling**: Comprehensive error handling with proper HTTP status codes

### Security Features

- **Credential Protection**: Never expose backend credentials to clients
- **Token Management**: Handle JWT and other authentication tokens
- **Request Sanitization**: Clean and validate all incoming requests
- **Access Control**: Enforce service-level access permissions
- **Audit Logging**: Log all proxy requests for security monitoring

### Development Features

- **Spring Boot DevTools**: Auto-restart and live reload with intelligent exclusions
- **Profile-based Configuration**: Separate dev and prod configurations
- **Debug Logging**: Detailed logging for development (dev profile only)
- **Automated Quality Checks**: Git hooks for pre-commit validation
- **Conventional Commits**: Enforced commit message format
- **Version Management**: Automated versioning with Axion Release
- **Code Formatting**: Automated code formatting with Spotless
- **Coverage Reporting**: JaCoCo integration for code coverage
- **Testing**: Unit and integration tests

## API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/proxy/{appId}/{terId}/{type}/{typeId}` | GET | Proxy request to protected service |
| `/actuator/health` | GET | Application health status |

### Usage Examples

#### Proxy Request to Protected Service

```bash
curl -X GET "http://localhost:8080/proxy/1/1/wms/123" \
  -H "Authorization: Bearer your-jwt-token" \
  -H "Content-Type: application/json"
```

#### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Response:

```json
{
  "status": "UP"
}
```

### Request Parameters

- `appId`: Application identifier (Integer)
- `terId`: Territory identifier (Integer)
- `type`: Service type (wms, sql) (String)
- `typeId`: Service instance identifier (Integer)
- `Authorization`: Bearer token (optional, automatically extracts token from "Bearer " prefix)
- Query parameters: Passed through to target service (Map<String, String>)

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SITMUN_BACKEND_CONFIG_URL` | URL to backend configuration service | Yes | - |
| `SITMUN_BACKEND_CONFIG_SECRET` | Secret key for configuration access | Yes | - |
| `SERVER_PORT` | Application port | No | 8080 |
| `SPRING_PROFILES_ACTIVE` | Spring profile to use | No | prod |

### Profiles

#### Development Profile (`dev`)

- Debug logging enabled
- H2 console available
- Detailed error messages
- Development tools enabled

#### Production Profile (`prod`)

- Minimal logging
- Security optimizations
- Performance tuning
- Production-ready configuration

### Configuration Files

#### Base Configuration (`src/main/resources/application.yml`)

```yaml
# Logging Configuration
logging:
  level:
    ROOT: INFO
    org.sitmun.proxy.middleware: INFO

# Sitmun Proxy Configuration
sitmun:
  backend:
    config:
      url: http://some.url
      secret: some-secret

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health
      base-path: /actuator
  endpoint:
    health:
      show-details: never
      show-components: never
  health:
    defaults:
      enabled: true
```

#### Development Profile (`src/main/resources/application-dev.yml`)

```yaml
# Development-specific configuration
logging:
  level:
    org.sitmun.proxy.middleware: DEBUG
    org.springframework.web: DEBUG

# Development tools
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

#### Production Profile (`src/main/resources/application-prod.yml`)

```yaml
# Production-specific configuration
logging:
  level:
    org.sitmun.proxy.middleware: INFO
    ROOT: WARN

# Production optimizations
spring:
  devtools:
    restart:
      enabled: false
    livereload:
      enabled: false
```

#### External Configuration (`config/application.yml`)

The application supports external configuration files mounted in Docker containers:

```yaml
# External Configuration for SITMUN Proxy Middleware
# This file is mounted from the host system into the container

# Logging Configuration
logging:
  level:
    ROOT: INFO
    org.sitmun.proxy.middleware: INFO
  file:
    name: /app/logs/sitmun-proxy-middleware.log
    max-size: 100MB
    max-history: 30

# SITMUN Backend Configuration
sitmun:
  backend:
    config:
      url: http://sitmun-backend:8080
      secret: ${SITMUN_BACKEND_CONFIG_SECRET:your-secret-key-here}

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    min-response-size: 1024

# HTTP Client Configuration
http:
  client:
    connect-timeout: 5000
    read-timeout: 10000
    max-connections: 200
    max-connections-per-route: 50
```

#### External Production Configuration (`config/application-prod.yml`)

```yaml
# External Production Configuration
logging:
  level:
    ROOT: WARN
    org.sitmun.proxy.middleware: INFO
  file:
    name: /app/logs/sitmun-proxy-middleware.log
    max-size: 100MB
    max-history: 30

# Production server configuration
server:
  port: 8080
```

## Architecture

### Technology Stack

- **Spring Boot 3.5.4**: Application framework with Spring Web, JDBC, and Actuator
- **Spring Web**: REST API support
- **Spring JDBC**: Database connectivity
- **Spring Actuator**: Health checks and monitoring
- **OkHttp 4.12.0**: HTTP client for service communication
- **JJWT 0.12.6**: JWT token handling with API, implementation, and Jackson modules
- **PostgreSQL/Oracle**: Database drivers for JDBC connections
- **H2 2.2.224**: In-memory database for testing
- **JSON 20240303**: JSON processing library
- **Gradle**: Build system with Version Catalogs
- **Docker**: Multi-stage containerization with Amazon Corretto
- **Spotless 7.2.0**: Code formatting with Google Java Format
- **JaCoCo**: Code coverage reporting
- **Axion Release 1.19.0**: Version management with semantic versioning
- **Lombok 8.6**: Reduces boilerplate code

### System Architecture

```
┌─────────────────┐    ┌─────────────────────┐    ┌─────────────────┐
│   SITMUN Map    │───▶│  Proxy Middleware   │───▶│ Protected       │
│     Viewer      │    │                     │    │   Services      │
└─────────────────┘    └─────────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │ SITMUN Backend  │
                       │     Core        │
                       └─────────────────┘
```

**Request Flow:**

1. Client sends request to `/proxy/{appId}/{terId}/{type}/{typeId}`
2. Proxy loads configuration from SITMUN Backend Core
3. Request is decorated and forwarded to target service
4. Response is decorated and returned to client

### Key Components

- **`Application.java`**: Main Spring Boot application class
- **`ProxyMiddlewareController`**: Main REST controller handling proxy requests
- **`RequestConfigurationService`**: Orchestrates request processing flow and configuration loading from SITMUN Backend Core
- **`RequestExecutorService`**: Handles request execution logic and protocol routing
- **`RequestExecutorFactory`**: Factory for creating request execution instances based on service type
- **Protocol Implementations**:
  - **HTTP**: `HttpRequestExecutor`, `HttpClientFactoryService`, `HttpRequestDecoratorAddBasicSecurity`, `HttpRequestDecoratorAddEndpoint`
  - **JDBC**: `JdbcRequestExecutor`, `JdbcRequestDecoratorAddConnection`, `JdbcRequestDecoratorAddQuery`
  - **WMS**: `WmsCapabilitiesResponseDecorator` for WMS capabilities processing
- **Decorator Pattern**: Flexible request/response modification through `RequestDecorator` and `ResponseDecorator` interfaces
- **DTO Classes**: Data transfer objects including `ConfigProxyDto`, `ConfigProxyRequestDto`, `ErrorResponseDto`, `HttpSecurityDto`, `PayloadDto`
- **Context Classes**: Protocol-specific contexts (`HttpContext`, `JdbcContext`) for request processing
- **Test Structure**: Protocol-specific tests (`ExecutionRequestExecutorServiceTest`) and utilities

### Request Processing Flow

1. **Request Reception**: `ProxyMiddlewareController` receives proxy request
2. **Token Extraction**: Extract JWT token from Authorization header
3. **Configuration Loading**: Load service configuration from SITMUN Backend Core using `RequestConfigurationService`
4. **Request Decoration**: Apply decorators to modify request based on service type
5. **Service Execution**: Forward request to target service using `RequestExecutionService`
6. **Response Decoration**: Apply decorators to modify response
7. **Response Return**: Return modified response to client

### Decorator Pattern

The service uses the decorator pattern to modify requests and responses:

```java
// Request decorators
HttpRequestDecoratorAddBasicSecurity    // Adds basic authentication to HTTP requests
HttpRequestDecoratorAddEndpoint         // Adds endpoint configuration to HTTP requests
JdbcRequestDecoratorAddConnection       // Adds database connection to JDBC requests
JdbcRequestDecoratorAddQuery           // Adds query configuration to JDBC requests

// Response decorators
WmsCapabilitiesResponseDecorator       // Modifies WMS capabilities responses
```

**Core Interfaces:**

- `RequestDecorator<T>`: Interface for request decorators
- `ResponseDecorator<T>`: Interface for response decorators
- `Decorator<T>`: Base decorator interface
- `Context`: Context interface for decorator operations

### Extensibility

- **Custom Decorators**: Implement new request/response decorators
- **Service Types**: Add support for new service types
- **Authentication**: Extend authentication mechanisms
- **Configuration**: Customize configuration loading

### Error Handling

- **HTTP Status Codes**: Proper status code mapping
- **Error Response Format**: Consistent error response structure
- **Logging**: Comprehensive error logging

## Development

### Profiles Explained

#### Development Profile

- Enhanced logging for debugging
- Development tools enabled
- H2 console for database management
- Detailed error messages

#### Production Profile

- Optimized for performance
- Minimal logging
- Security hardening
- Production monitoring

### Deployment

#### Docker Deployment

```bash
# Build and run with Docker Compose
cd docker/development
docker-compose up --build

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**Docker Configuration:**

- **Multi-stage build** using Amazon Corretto 17 (`docker/Dockerfile`)
- **Development environment** with Docker Compose (`docker/development/docker-compose.yml`)
- **Health checks** with curl-based monitoring
- **External configuration** mounting from host
- **JVM optimization** with G1GC and container support
- **Volume mounting** for logs and configuration

#### Manual Deployment

```bash
# Build JAR
./gradlew build

# Run with production profile
java -jar build/libs/sitmun-proxy-middleware.jar --spring.profiles.active=prod
```

### Project Structure

```
sitmun-proxy-middleware/
├── src/
│   ├── main/
│   │   ├── java/org/sitmun/proxy/middleware/
│   │   │   ├── Application.java                    # Main application class
│   │   │   ├── config/                            # Configuration classes
│   │   │   ├── controllers/                       # REST controllers
│   │   │   ├── decorator/                         # Request/response decorators
│   │   │   ├── dto/                               # Data transfer objects
│   │   │   ├── protocols/                         # Protocol implementations
│   │   │   │   ├── http/                          # HTTP protocol support
│   │   │   │   ├── jdbc/                          # JDBC protocol support
│   │   │   │   └── wms/                           # WMS protocol support (uses HTTP)
│   │   │   ├── service/                           # Business logic services
│   │   │   └── utils/                             # Utility classes
│   │   └── resources/
│   │       ├── application.yml                     # Base configuration
│   │       ├── application-dev.yml                 # Development profile
│   │       ├── application-prod.yml                # Production profile
│   │       └── META-INF/                          # Spring configuration metadata
│   │           └── additional-spring-configuration-metadata.json
│   └── test/
│       ├── java/org/sitmun/proxy/middleware/
│       │   ├── protocols/                         # Protocol-specific tests
│       │   │   ├── http/                          # HTTP protocol tests
│       │   │   ├── jdbc/                          # JDBC protocol tests
│       │   │   └── wms/                           # WMS protocol tests
│       │   ├── service/                           # Service layer tests
│       │   ├── decorator/                         # Decorator tests (empty)
│       │   └── test/                              # Test utilities and fixtures
│       │       ├── dto/                           # Test DTOs
│       │       ├── fixtures/                      # Test data fixtures
│       │       ├── interceptors/                  # Test interceptors
│       │       ├── service/                       # Test service implementations (empty)
│       │       ├── TestUtils.java                 # Test utilities
│       │       └── URIConstants.java              # URI constants for tests
│       └── resources/
│           └── application.yml                     # Test configuration
├── config/                                        # External configuration
│   ├── application.yml                             # External base config
│   └── application-prod.yml                        # External production config
├── docker/                                        # Docker configuration
│   ├── Dockerfile                                  # Multi-stage build with Amazon Corretto
│   └── development/
│       └── docker-compose.yml                     # Development environment
├── gradle/                                        # Gradle configuration
│   ├── libs.versions.toml                         # Version catalog for dependencies
│   └── wrapper/                                   # Gradle wrapper files
├── build.gradle                                   # Main build configuration
├── settings.gradle                                # Project settings
├── gradle.properties                              # Gradle properties
├── gradlew                                        # Gradle wrapper script (Unix)
├── gradlew.bat                                    # Gradle wrapper script (Windows)
```

### Key Components

- **`Application.java`**: Main Spring Boot application class
- **`ProxyMiddlewareController`**: Main REST controller handling proxy requests
- **`RequestConfigurationService`**: Orchestrates request processing flow and configuration loading from SITMUN Backend Core
- **`RequestExecutorService`**: Handles request execution logic and protocol routing
- **`RequestExecutorFactory`**: Factory for creating request execution instances based on service type
- **Protocol Implementations**:
  - **HTTP**: `HttpRequestExecutor`, `HttpClientFactoryService`, `HttpRequestDecoratorAddBasicSecurity`, `HttpRequestDecoratorAddEndpoint`
  - **JDBC**: `JdbcRequestExecutor`, `JdbcRequestDecoratorAddConnection`, `JdbcRequestDecoratorAddQuery`
  - **WMS**: `WmsCapabilitiesResponseDecorator` for WMS capabilities processing
- **Decorator Pattern**: Flexible request/response modification through `RequestDecorator` and `ResponseDecorator` interfaces
- **DTO Classes**: Data transfer objects including `ConfigProxyDto`, `ConfigProxyRequestDto`, `ErrorResponseDto`, `HttpSecurityDto`, `PayloadDto`
- **Context Classes**: Protocol-specific contexts (`HttpContext`, `JdbcContext`) for request processing
- **Test Structure**: Comprehensive testing with protocol-specific test classes (`ExecutionRequestExecutorServiceTest` for each protocol) and utilities

### Build System

The project uses Gradle with Version Catalogs for dependency management:

- **Version Catalog**: `gradle/libs.versions.toml` - Centralized dependency versions
- **Packaging**: Configurable JAR or WAR output via `-Ppackaging` property
- **Plugins**: Spring Boot 3.5.4, Lombok 8.6, Spotless 7.2.0, Axion Release 1.19.0
- **Quality Tools**: JaCoCo for coverage, Spotless for formatting
- **Dependencies**:
  - Spring Boot Starters (Web, JDBC, Actuator)
  - OkHttp 4.12.0 for HTTP client
  - JJWT 0.12.6 for JWT handling
  - PostgreSQL and Oracle JDBC drivers
  - H2 2.2.224 for testing
  - JSON 20240303 for JSON processing

### Code Quality

Code quality tools:

- **Spotless**: Code formatting with Google Java Format
- **JaCoCo**: Code coverage reporting
- **Axion Release**: Version management with semantic versioning
- **Git Hooks**: Automated quality checks and commit validation

### Running Quality Checks

```bash
# Format code
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# Check code coverage
./gradlew jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Version Management

The project uses Axion Release for automated version management:

```bash
# Check current version
./gradlew currentVersion

# Create a new release
./gradlew release

# Create a new patch version
./gradlew patch
```

#### Creating a Release

**Prerequisites:**

1. **Clean Git State**: Ensure all changes are committed
2. **Working Directory**: No uncommitted changes
3. **Git Repository**: Must be a valid Git repository

**Step-by-Step Release Process:**

```bash
# 1. Check current Git status
git status

# 2. Add and commit any pending changes
git add .
git commit -m "docs: update documentation for release"

# 3. Verify the repository is clean
git status

# 4. Check current version
./gradlew currentVersion

# 5. Create a new release
./gradlew release

# 6. Push the release tag
git push --tags
```

**Release Types:**

- `./gradlew release`: Creates a new patch version (e.g., 1.0.0 → 1.0.1)
- `./gradlew release -Prelease.scope=minor`: Creates a new minor version (e.g., 1.0.0 → 1.1.0)
- `./gradlew release -Prelease.scope=major`: Creates a new major version (e.g., 1.0.0 → 2.0.0)

### Testing

Testing commands:

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ProxyMiddlewareControllerTest

# Run protocol-specific tests
./gradlew test --tests *HttpExecutionRequestExecutorServiceTest
./gradlew test --tests *JdbcExecutionRequestExecutorServiceTest
./gradlew test --tests *WmsExecutionRequestExecutorServiceTest

# Run service tests
./gradlew test --tests *ServiceExecutionRequestExecutorServiceTest

# Run tests with coverage
./gradlew test jacocoTestReport
```

#### Test Coverage

- **Protocol Tests**: Each protocol (HTTP, JDBC, WMS) has dedicated test classes
  - `ExecutionRequestExecutorServiceTest` for each protocol
  - `HttpClientFactoryServiceTest` for HTTP client factory
- **Service Tests**: `ExecutionRequestExecutorServiceTest` for service layer testing
- **Test Utilities**:
  - `TestUtils` for common test functionality
  - `URIConstants` for test URI constants
  - `AuthorizationProxyFixtures` for test data fixtures
  - Test interceptors for request/response simulation
- **Test DTOs**: `AuthenticationResponse` and `UserPasswordAuthenticationRequest` for testing
- **Edge Cases**: Boundary conditions and error handling

### Development Workflow

#### Git Hooks

Automated Git hooks run on every commit:

**Pre-commit checks:**

- Code formatting validation (Spotless)
- Unit and integration tests
- Code coverage verification

**Commit message validation:**

- Conventional commit format enforcement
- SITMUN-specific scope support `(proxy)`

#### Commit Message Format

Follow the conventional commit format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test changes
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD changes
- `build`: Build system changes

**Examples:**

```bash
git commit -m "feat(proxy): add request decorator functionality"
git commit -m "fix(proxy): resolve authentication token handling"
git commit -m "docs: update README with proxy configuration info"
git commit -m "test: add integration tests for proxy requests"
git commit -m "style: format code with Google Java Format"
```

#### Managing Git Hooks

```bash
# Install Git hooks (automatic with build)
./gradlew setupGitHooks

# Remove Git hooks
./gradlew removeGitHooks
```

## Advanced Features

### Security and Authentication

Security features:

- **JWT Token Handling**: Validate and parse tokens
- **Credential Protection**: Never expose backend credentials
- **Request Sanitization**: Clean and validate all incoming requests
- **Access Control**: Enforce service-level permissions
- **Audit Logging**: Request logging

### Monitoring and Observability

- **Spring Boot Actuator**: Health checks, metrics, and application monitoring
- **Custom Health Indicators**: Proxy service health monitoring
- **Request Tracking**: Request monitoring
- **Error Handling**: Error handling and logging
- **Performance Metrics**: Request timing and metrics

#### Actuator Endpoints

| Endpoint | Description | Access |
|----------|-------------|--------|
| `/actuator/health` | Application health status | Public |

**Health Check Response:**

```json
{
  "status": "UP"
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes following the conventional commit format
4. Add tests for new functionality
5. Ensure all tests pass and code is formatted
6. Submit a pull request

### Development Guidelines

- Follow the conventional commit format
- Write tests for new functionality
- Ensure code coverage remains high
- Run quality checks before committing
- Update documentation as needed

## Integration with SITMUN

Provides proxy capabilities for the [SITMUN](https://github.com/sitmun/) platform and can be deployed alongside other components.

### Prerequisites

Prerequisites:

- **SITMUN Backend Core** running and accessible
- **SITMUN Map Viewer** configured to use the proxy
- **Network connectivity** between all SITMUN components
- **Shared secret key** for secure communication

### Configuration Steps

#### 1. SITMUN Backend Core Configuration

The Proxy Middleware requires configuration from the SITMUN Backend Core. Ensure your backend is configured to provide proxy configuration:

```yaml
# SITMUN Backend Core configuration
sitmun:
  backend:
    proxy:
      enabled: true
      secret: ${SITMUN_PROXY_SECRET:your-shared-secret}
      endpoints:
        - /api/config/proxy
```

#### 2. Proxy Middleware Configuration

Configure the Proxy Middleware to connect to your SITMUN Backend Core:

```bash
# Environment variables for Docker deployment
SITMUN_BACKEND_CONFIG_URL=http://sitmun-backend:8080/api/config/proxy
SITMUN_BACKEND_CONFIG_SECRET=your-shared-secret
```

Or in `application.yml`:

```yaml
sitmun:
  backend:
    config:
      url: http://sitmun-backend:8080/api/config/proxy
      secret: your-shared-secret
```

#### 3. SITMUN Map Viewer Configuration

Configure the SITMUN Map Viewer to use the Proxy Middleware for protected services:

```javascript
// Map Viewer configuration
const mapViewerConfig = {
  proxy: {
    enabled: true,
    baseUrl: 'http://localhost:8080/proxy',
    authentication: {
      type: 'bearer',
      token: 'your-jwt-token'
    }
  },
  services: {
    wms: {
      useProxy: true,
      proxyPath: '/{appId}/{terId}/wms/{serviceId}'
    },
    jdbc: {
      useProxy: true,
      proxyPath: '/{appId}/{terId}/jdbc/{serviceId}'
    }
  }
};
```

#### 4. Network Configuration

Ensure proper network connectivity between components:

```yaml
# Docker Compose network configuration
services:
  sitmun-backend:
    # ... backend configuration
    networks:
      - sitmun-network
  
  sitmun-proxy-middleware:
    # ... proxy configuration
    networks:
      - sitmun-network
    environment:
      - SITMUN_BACKEND_CONFIG_URL=http://sitmun-backend:8080/api/config/proxy
      - SITMUN_BACKEND_CONFIG_SECRET=your-shared-secret

networks:
  sitmun-network:
    driver: bridge
```

### Service Types and Configuration

The Proxy Middleware supports different service types that can be configured in the SITMUN Backend Core:

#### WMS Services

```json
{
  "type": "wms",
  "url": "http://protected-wms-service/wms",
  "layers": ["layer1", "layer2"],
  "authentication": {
    "type": "basic",
    "username": "protected_user",
    "password": "protected_pass"
  }
}
```

#### JDBC Services

```json
{
  "type": "jdbc",
  "url": "jdbc:postgresql://protected-db:5432/database",
  "username": "db_user",
  "password": "db_password",
  "query": "SELECT * FROM spatial_data WHERE territory_id = ?"
}
```

### Security Configuration

Basic security features cover proxy authentication and request handling.

### Monitoring and Health Checks

#### Health Check Configuration

```yaml
# Health check configuration for SITMUN integration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
  health:
    defaults:
      enabled: true
    indicators:
      sitmun-backend:
        enabled: true
```

#### Logging Configuration

```yaml
# Logging configuration for SITMUN integration
logging:
  level:
    org.sitmun.proxy.middleware: INFO
    org.sitmun: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Troubleshooting Integration

#### Common Issues

1. **Connection Refused to SITMUN Backend**

   ```bash
   # Check if backend is running
   curl http://sitmun-backend:8080/actuator/health
   
   # Verify network connectivity
   docker exec sitmun-proxy-middleware ping sitmun-backend
   ```

2. **Authentication Failures**

   ```bash
   # Check shared secret configuration
   echo $SITMUN_BACKEND_CONFIG_SECRET
   
   # Verify JWT token format
   curl -H "Authorization: Bearer your-token" http://localhost:8080/proxy/1/1/test/1
   ```

3. **Service Configuration Not Found**

   ```bash
   # Check backend configuration endpoint
   curl http://sitmun-backend:8080/api/config/proxy
   
   # Verify service configuration in backend
   curl -H "Authorization: Bearer your-token" http://sitmun-backend:8080/api/services
   ```

#### Debug Mode

```bash
# Enable debug logging for integration issues
export LOGGING_LEVEL_ORG_SITMUN_PROXY_MIDDLEWARE=DEBUG
export LOGGING_LEVEL_ORG_SITMUN=DEBUG

# Restart the proxy middleware
docker-compose restart sitmun-proxy-middleware
```

### SITMUN Application Stack

See [SITMUN Application Stack](https://github.com/sitmun/sitmun-application-stack) as an example of how to deploy and run the proxy as part of the SITMUN stack.

## Support

For questions and support:

- Open an issue on GitHub
- Check the [SITMUN documentation](https://sitmun.github.io/)
- Join the SITMUN community discussions

## License

This project uses the following license: [European Union Public Licence V. 1.2](LICENSE).
