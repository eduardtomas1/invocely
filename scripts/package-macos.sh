#!/usr/bin/env bash

set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
version="1.0.0"
jar_name="invocely-${version}.jar"
input_dir="${project_dir}/target/jpackage-input"
output_dir="${project_dir}/dist"
machine_arch="$(uname -m)"

case "${machine_arch}" in
  arm64) artifact_arch="apple-silicon" ;;
  x86_64) artifact_arch="intel" ;;
  *) artifact_arch="${machine_arch}" ;;
esac

cd "${project_dir}"
mvn -q -DskipTests package

mkdir -p "${input_dir}" "${output_dir}"
cp "${project_dir}/target/${jar_name}" "${input_dir}/${jar_name}"

jpackage \
  --type dmg \
  --name Invoicely \
  --app-version "${version}" \
  --vendor "Eduard Tomas" \
  --description "Offline invoices and quotes, without the fuss." \
  --input "${input_dir}" \
  --main-jar "${jar_name}" \
  --main-class app.invocely.InvoicelyApp \
  --icon "${project_dir}/src/main/resources/icon/invocely.icns" \
  --mac-package-identifier app.invocely.desktop \
  --dest "${output_dir}"

generated_dmg="${output_dir}/Invoicely-${version}.dmg"
final_dmg="${output_dir}/Invoicely-${version}-macos-${artifact_arch}.dmg"
mv "${generated_dmg}" "${final_dmg}"

echo "Created ${final_dmg}"
