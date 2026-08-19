#!/bin/sh -
#
# Build and run the migrated Quarkus "treen" backend as a Docker container.
#
# This replaces the previous TomEE deploy flow (manager-text undeploy/deploy of
# a WAR). The application is now a self-contained Quarkus image.
#
# Environment variables (optional):
#   IMAGE_TAG      Docker image tag           (default: treen:latest)
#   CONTAINER_NAME Docker container name       (default: treen)
#   HOST_PORT      Host port to publish 8080   (default: 8080; use 0 for random)
#   SEED_DEMO_USER If "true", seed a demo user (login: demo / password: demo1234)

app_name="treen"
image_tag="${IMAGE_TAG:-${app_name}:latest}"
container_name="${CONTAINER_NAME:-${app_name}}"
host_port="${HOST_PORT:-8080}"
seed_demo_user="${SEED_DEMO_USER:-false}"

script_file="$(realpath "$0")"
base_dir="$(dirname "${script_file}")"

throw_error() {
    echo "$1" >&2
    exit 1
}

echo "---- BUILDING DOCKER IMAGE ${image_tag} ----"
cd "${base_dir}" && docker build -t "${image_tag}" . ||\
    throw_error "FAILED TO BUILD DOCKER IMAGE"
echo "---- SUCCESS ----"

echo "---- (RE)STARTING CONTAINER ${container_name} ----"
docker rm -f "${container_name}" >/dev/null 2>&1
docker run -d --name "${container_name}" \
    -p "${host_port}:8080" \
    -e "TREEN_DEMO_SEED_USER_ENABLED=${seed_demo_user}" \
    "${image_tag}" ||\
    throw_error "FAILED TO START CONTAINER"

assigned_port="$(docker port "${container_name}" 8080 | head -1 | sed 's/.*://')"
echo "---- SUCCESS ----"
echo "treen is running; container ${container_name}, port ${assigned_port}"
echo "Health: http://localhost:${assigned_port}/q/health"
