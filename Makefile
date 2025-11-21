.PHONY: help dev build-jvm run-jvm build-native run-native clean test

# Default target
help:
	@echo "Available targets:"
	@echo "  dev        - Run in development mode (JVM)"
	@echo "  build-jvm  - Build for JVM production"
	@echo "  run-jvm    - Run JVM production build"
	@echo "  build-native - Build native image"
	@echo "  run-native - Run native image"
	@echo "  clean      - Clean build artifacts"
	@echo "  test       - Run tests"

# Development mode (JVM)
dev:
	mvn quarkus:dev -f ledger

# JVM production build
build-jvm:
	mvn clean package -DskipTests

# Run JVM production build
run-jvm: build-jvm
	java -jar ledger/target/quarkus-app/quarkus-run.jar

# Native image build
build-native:
	mvn clean package -Pnative -DskipTests

# Run native image
run-native: build-native
	./ledger/target/ledger-1.0.0-SNAPSHOT-runner

# Clean build artifacts
clean:
	mvn clean

# Run tests
test:
	mvn test

api-test:
	sh scripts/test.sh
