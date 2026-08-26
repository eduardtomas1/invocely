#!/usr/bin/env bash

set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
maven_cmd="${project_dir}/mvnw"
if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "macOS packaging must run on a Mac." >&2
  exit 1
fi
if ! jpackage --version >/dev/null 2>&1; then
  if command -v brew >/dev/null 2>&1; then
    brew_jdk="$(brew --prefix openjdk 2>/dev/null || true)"
    if [[ -x "${brew_jdk}/bin/jpackage" ]]; then
      export JAVA_HOME="${brew_jdk}"
      export PATH="${brew_jdk}/bin:${PATH}"
    fi
  fi
fi
if ! jpackage --version >/dev/null 2>&1; then
  echo "jpackage was not found. Install JDK 17 or newer and try again." >&2
  exit 1
fi

version="$("${maven_cmd}" -q -DforceStdout help:evaluate -Dexpression=project.version)"
jar_name="invocely-${version}-standalone.jar"
output_dir="${project_dir}/dist"
machine_arch="$(uname -m)"
package_dir="$(mktemp -d "${TMPDIR:-/tmp}/invocely-package.XXXXXX")"
input_dir="${package_dir}/input"
package_output_dir="${package_dir}/output"

cleanup() {
  rm -rf "${package_dir}"
}
trap cleanup EXIT

case "${machine_arch}" in
  arm64) artifact_arch="apple-silicon" ;;
  x86_64) artifact_arch="intel" ;;
  *) artifact_arch="${machine_arch}" ;;
esac

cd "${project_dir}"
"${maven_cmd}" -B -ntp verify

mkdir -p "${input_dir}" "${package_output_dir}" "${output_dir}"
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
  --add-modules java.base,java.compiler,java.desktop,java.prefs,java.sql.rowset,jdk.xml.dom,jdk.unsupported,jdk.localedata \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --icon "${project_dir}/src/main/resources/icon/invocely.icns" \
  --resource-dir "${project_dir}/packaging/macos" \
  --mac-package-identifier app.invocely.desktop \
  --mac-app-category business \
  --dest "${package_output_dir}"

generated_dmg="${package_output_dir}/Invoicely-${version}.dmg"
final_dmg="${output_dir}/Invoicely-${version}-macos-${artifact_arch}.dmg"
mv -f "${generated_dmg}" "${final_dmg}"

echo "Created ${final_dmg}"
