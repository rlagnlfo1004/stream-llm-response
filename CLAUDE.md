# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 4.0.3 application (`streaming-demo`) for demonstrating streaming capabilities. The server module uses Java 21 with Spring MVC (not WebFlux).

## Commands

All commands should be run from the `server/` directory.

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew bootRun
```

### Test
```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.mailsangja.streamingdemo.SomeTestClass"
```

### Clean
```bash
./gradlew clean
```

## Architecture

- **Framework**: Spring Boot 4.0.3 with `spring-boot-starter-webmvc` (synchronous MVC, not reactive WebFlux)
- **Java**: 21
- **Build**: Gradle 9.3.1 with Spring dependency management
- **Package**: `com.mailsangja.streamingdemo`
- **Lombok**: Available for boilerplate reduction

The project is in early stages — only the main application entry point exists. New controllers, services, and models should be placed under `server/src/main/java/com/mailsangja/streamingdemo/`.
