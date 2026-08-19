#!/bin/sh -
#
# Build script for treen.
#
# After the Jakarta EE -> Quarkus migration the backend is no longer a WAR
# deployed to TomEE; it is a self-contained Quarkus application (fast-jar).
# This script builds the Oracle JET frontend (if the `ojet` tooling is
# available) and the Quarkus backend. For a container image use the Dockerfile
# in the repository root instead (see build-deploy.sh).

app_name="treen"
script_file="$(realpath "$0")"
base_dir="$(dirname "${script_file}")"
fe_dir="${base_dir}/frontend"
be_dir="${base_dir}/backend"

throw_error() {
    echo "$1" >&2
    exit 1
}

if command -v ojet >/dev/null 2>&1; then
    echo "---- BUILDING FRONTEND ----"
    cd "${fe_dir}" && ojet clean web && ojet build --release ||\
        throw_error "FAILED TO BUILD FRONTEND"
    echo "---- SUCCESS ----"
else
    echo "---- SKIPPING FRONTEND (ojet tooling not found) ----"
fi

echo "---- BUILDING BACKEND (Quarkus) ----"
cd "${be_dir}" && mvn -B clean package ||\
    throw_error "FAILED TO BUILD BACKEND"
echo "---- SUCCESS ----"

echo "Runnable application: ${be_dir}/target/quarkus-app/quarkus-run.jar"
echo "Run it with: java -jar ${be_dir}/target/quarkus-app/quarkus-run.jar"
