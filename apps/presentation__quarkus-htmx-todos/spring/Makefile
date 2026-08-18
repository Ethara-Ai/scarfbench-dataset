# ScarfBench container harness — quarkus-htmx-todos / spring
#
# `scarf validate` copies this file (plus Dockerfile, smoke.py and metadata.json) into a
# candidate's output/ directory and runs `make test` there, so every target below has to
# work from a bare checkout with nothing but Docker on PATH.
#
# Required targets: build rebuild up logs down test local

IMAGE_NAME      ?= scarfbench-htmx-todos-spring
CONTAINER_NAME  ?= scarfbench-htmx-todos-spring-app
APP_PORT        ?= 8080
BASE_URL        ?= http://localhost:$(APP_PORT)

# Spring Boot readiness signal; Quarkus would be "started in.*Listening on:",
# Jakarta/Open Liberty CWWKF0011I.
READY_PATTERN   ?= Tomcat started on port|Started .* in .* seconds
STARTUP_TIMEOUT ?= 300

.PHONY: build rebuild up logs down test local clean

build:
	docker build -t $(IMAGE_NAME) .

rebuild:
	docker build --no-cache -t $(IMAGE_NAME) .

up: build
	-@docker rm -f $(CONTAINER_NAME) >/dev/null 2>&1
	docker run -d --name $(CONTAINER_NAME) $(IMAGE_NAME)
	@echo "waiting for app to start..."
	@elapsed=0; \
	while [ $$elapsed -lt $(STARTUP_TIMEOUT) ]; do \
	  if docker logs $(CONTAINER_NAME) 2>&1 | grep -qE '$(READY_PATTERN)'; then \
	    echo "Application started and ready."; \
	    exit 0; \
	  fi; \
	  if docker logs $(CONTAINER_NAME) 2>&1 | grep -q 'BUILD FAILURE'; then \
	    echo "BUILD FAILURE detected while starting the application:"; \
	    docker logs --tail 80 $(CONTAINER_NAME) 2>&1; \
	    exit 1; \
	  fi; \
	  if [ "$$(docker inspect -f '{{.State.Running}}' $(CONTAINER_NAME) 2>/dev/null)" != "true" ]; then \
	    echo "container exited before becoming ready:"; \
	    docker logs --tail 80 $(CONTAINER_NAME) 2>&1; \
	    exit 1; \
	  fi; \
	  sleep 2; elapsed=$$((elapsed + 2)); \
	done; \
	echo "timed out after $(STARTUP_TIMEOUT)s waiting for the readiness pattern"; \
	docker logs --tail 80 $(CONTAINER_NAME) 2>&1; \
	exit 1

logs:
	docker logs $(CONTAINER_NAME)

down:
	-@docker rm -f $(CONTAINER_NAME) >/dev/null 2>&1 || true
	@echo "container removed"

# The graded entry point. Tests run inside the container via `docker exec`, so no host
# port is published (F4/F5). The container is removed whatever the outcome.
test: up
	@echo "running behavioural oracle against $(BASE_URL)"
	@docker cp smoke.py $(CONTAINER_NAME):/app/smoke.py
	@if [ -f test.sh ]; then docker cp test.sh $(CONTAINER_NAME):/app/test.sh; fi
	@set +e; \
	docker exec -e BASE_URL=$(BASE_URL) $(CONTAINER_NAME) /opt/venv/bin/python -m pytest -v -p no:randomly /app/smoke.py; \
	rc=$$?; \
	echo "--- application log (tail) ---"; \
	docker logs --tail 40 $(CONTAINER_NAME) 2>&1; \
	docker rm -f $(CONTAINER_NAME) >/dev/null 2>&1; \
	exit $$rc

# Same oracle, no Docker: build and run the application on the host, then probe it.
# Requires JDK 21 + Maven on PATH.
local:
	mvn clean package -DskipTests
	@set -e; \
	java -jar target/todos-1.0.0-SNAPSHOT.jar > /tmp/$(IMAGE_NAME)-local.log 2>&1 & \
	app_pid=$$!; \
	trap "kill $$app_pid 2>/dev/null || true" EXIT; \
	elapsed=0; \
	while [ $$elapsed -lt $(STARTUP_TIMEOUT) ]; do \
	  if grep -qE '$(READY_PATTERN)' /tmp/$(IMAGE_NAME)-local.log; then break; fi; \
	  sleep 2; elapsed=$$((elapsed + 2)); \
	done; \
	echo "Application started and ready."; \
	BASE_URL=$(BASE_URL) ./test.sh

clean: down
	-@docker rmi $(IMAGE_NAME) >/dev/null 2>&1 || true
