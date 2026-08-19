#!/bin/sh -

app_name="treen"
script_file="$(realpath $0)"
base_dir="$(dirname ${script_file})"
fe_dir="${base_dir}/frontend"
be_dir="${base_dir}/backend"
build_dir="${base_dir}/target"

throw_error() {
    echo "$1" >&2
    exit 1
}

echo "---- BUILDING FRONTEND ----"
cd ${fe_dir} && ojet clean web && ojet build --release ||\
    throw_error "FAILED TO BUILD FRONTEND"
echo "---- SUCCESS ----"

echo "---- BUILDING BACKEND ----"
cd ${be_dir} && mvn clean package ||\
    throw_error "FAILED TO BUILD BACKEND"
echo "---- SUCCESS ----"

echo "---- PACKAGING BACKEND AND FRONTEND ----"
rm -rf "${build_dir}" && mkdir "${build_dir}" && cd "${build_dir}" ||\
    throw_error "FAILED TO CREATE A CLEAN PACKAGING DIR ${build_dir}"
find "${be_dir}/target" -name "${app_name}*.war" \
    -exec tar -xf \{\} \; ||\
    throw_error "FAILED TO UNPACK THE WAR FILE"
cp -r "${fe_dir}/web/" "./" ||\
    throw_error "FAILED TO COPY FRONTEND FILES"
cd "${build_dir}" && zip -r ../treen.war * ||\
    throw_error "FAILED TO CREATE A WAR ARCHIVE"
echo "Complete package is ${base_dir}/treen.war file"
echo "---- SUCCESS ----"

