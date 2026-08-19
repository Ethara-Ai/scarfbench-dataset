# Validation harness for whole_applications/treen (target: quarkus)
APP_NAME        := treen-quarkus
IMAGE_NAME      := $(APP_NAME):validate
CONTAINER_NAME  := $(APP_NAME)-validate
HOST_PORT       := 18086
SUCCESS_PATTERN := Listening on:|started in|Installed features
FAILURE_PATTERN := BUILD FAILURE|\\[FATAL\\]|Failed to start|Unable to access jarfile|stopped in
SHELL := /bin/bash

default:
	@make help || make --help

compile:
	@echo "=== COMPILE START ==="
	@if [ ! -f Dockerfile ]; then echo "[ERROR] no Dockerfile produced by the agent"; echo "=== COMPILE FAIL ==="; exit 1; fi
	@if DOCKER_BUILDKIT=1 docker build -f Dockerfile -t $(IMAGE_NAME) . 2>&1; then \
		echo "[INFO] Built image: $(IMAGE_NAME)"; echo "=== COMPILE OK ==="; \
	else echo "=== COMPILE FAIL ==="; exit 1; fi

deploy: compile
	@echo "=== DEPLOY START ==="
	-@docker rm -f $(CONTAINER_NAME) >/dev/null 2>&1 || true
	docker run -d --name $(CONTAINER_NAME) -p $(HOST_PORT):8080 $(IMAGE_NAME)
	@elapsed=0; timeout=120; \
	while ! docker logs $(CONTAINER_NAME) 2>&1 | grep -Eq "$(FAILURE_PATTERN)|$(SUCCESS_PATTERN)"; do \
		sleep 2; elapsed=$$((elapsed + 2)); \
		if ! docker ps --format '{{.Names}}' | grep -q "^$(CONTAINER_NAME)$$"; then echo "[WARN] container exited after $${elapsed}s"; break; fi; \
		if [ $$elapsed -ge $$timeout ]; then echo "[WARN] readiness timeout"; break; fi; \
	done; \
	if docker logs $(CONTAINER_NAME) 2>&1 | grep -Eq "$(FAILURE_PATTERN)"; then \
		docker logs --tail 40 $(CONTAINER_NAME) 2>&1; echo "=== DEPLOY FAIL ==="; exit 1; fi; \
	if ! docker logs $(CONTAINER_NAME) 2>&1 | grep -Eq "$(SUCCESS_PATTERN)"; then \
		docker logs --tail 40 $(CONTAINER_NAME) 2>&1; echo "=== DEPLOY FAIL ==="; exit 1; fi
	@echo "[INFO] container startup log:"; docker logs --tail 40 $(CONTAINER_NAME) 2>&1 || true
	@echo "=== DEPLOY OK ==="

test: deploy
	@echo "=== TEST START ==="
	@elapsed=0; until curl -s -o /dev/null http://localhost:18086/; do sleep 1; elapsed=$$((elapsed+1)); if [ $$elapsed -ge 30 ]; then break; fi; done
	@BASE_URL=http://localhost:18086 uv run --with pytest --with requests -- pytest -v smoke.py; rc=$$?; \
	$(MAKE) down; \
	if [ $$rc -eq 0 ]; then echo "=== TEST OK ==="; else echo "=== TEST FAIL ==="; fi; \
	exit $$rc

down:
	- docker rm -f $(CONTAINER_NAME) >/dev/null 2>&1 || true

clean: down
	- docker rmi $(IMAGE_NAME) 2>/dev/null
