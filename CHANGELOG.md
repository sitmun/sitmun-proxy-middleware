# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.1] - 2025-08-28

### Added

- HTTP POST request support for proxy endpoints
- Enhanced HTTP protocol interface with method and body accessor methods
- Comprehensive test suite for HTTP protocol components with SSL/TLS testing
- Extended test coverage for request configuration services

### Changed

- Refactored HTTP request body decorator to use generic context interface
- Enhanced proxy controller with POST request handling capabilities
- Improved HTTP request processing and execution
- Updated request configuration DTOs for better request support

### Removed

- Apache HTTP Client 5.x dependency and related configuration

### Fixed

- Improved HTTP request body handling and processing
- Enhanced request method detection and routing

## [1.1.0] - 2025-08-03

### Added

- Protocol-specific request executors for HTTP, JDBC, and WMS services
- Comprehensive code quality tools with Spotless and enhanced JaCoCo
- Git hooks for automated code quality enforcement and conventional commit validation
- Docker configuration with multi-stage builds using Amazon Corretto 17
- External configuration mounting for containerized deployments (replaced basic docker-compose.yml)
- Spring configuration metadata for better IDE support
- Gradle Version Catalog for centralized dependency management
- Axion Release plugin for automated version management

### Changed

- Migrated to Spring Boot 3.5.4 & Java 17
- Migrated dependencies to Version Catalog
- Reorganized codebase into protocol-based architecture (http, jdbc, wms)
- Completely rewrote documentation with detailed architecture guide
- Improved test organization with protocol-specific test classes
- Restructured Docker configuration with environment-specific configs

### Fixed

- Modernized SITMUN proxy middleware configuration and deployment structure
- Improved request processing and error handling
- Enhanced backward compatibility with existing APIs
- Build system with quality gates and automated checks

## [1.0.0] - 2024-11-12

### Added

- Initial stable release of SITMUN Proxy Middleware
- Spring Boot 2.7.18 application with proxy functionality
- Decorator pattern implementation for flexible request/response modification
- JWT token handling and authentication support (JJWT 0.12.6)
- REST API with proxy endpoint `/proxy/{appId}/{terId}/{type}/{typeId}`
- Spring Boot Actuator for health monitoring and application metrics
- Docker support with basic containerization (docker-compose.yml)
- OkHttp 4.12.0-based HTTP client
- Request sanitization and access control
- Error handling with proper HTTP status codes
- Comprehensive test suite with H2 database for testing
- Decorator-based architecture with HTTP and JDBC context support

### Changed

- Modernized from legacy proxy implementations
- Implemented proper dependency management
- Enhanced code quality and maintainability

### Fixed

- Various bug fixes and improvements from development phase

[Unreleased]: https://github.com/sitmun/sitmun-proxy-middleware/compare/sitmun-proxy-middleware/1.1.1...HEAD

[1.1.1]: https://github.com/sitmun/sitmun-proxy-middleware/compare/sitmun-proxy-middleware/1.1.0...sitmun-proxy-middleware/1.1.1

[1.1.0]: https://github.com/sitmun/sitmun-proxy-middleware/compare/sitmun-proxy-middleware/1.0.0...sitmun-proxy-middleware/1.1.0

[1.0.0]: https://github.com/sitmun/sitmun-proxy-middleware/releases/tag/sitmun-proxy-middleware/1.0.0
