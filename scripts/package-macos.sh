#!/usr/bin/env bash

set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
maven_cmd="${project_dir}/mvnw"
skip_build=false

if (( $# > 1 )); then
  echo "Usage: $0 [--skip-build]" >&2
  exit 2
fi
if (( $# == 1 )); then
  if [[ "$1" != "--skip-build" ]]; then
    echo "Usage: $0 [--skip-build]" >&2
    exit 2
  fi
  skip_build=true
fi

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
signing_identity="${INVOCELY_MAC_SIGNING_IDENTITY:-}"
signing_keychain="${INVOCELY_MAC_SIGNING_KEYCHAIN:-}"
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
if [[ "${skip_build}" == false ]]; then
  "${maven_cmd}" -B -ntp verify
elif [[ ! -s "${project_dir}/target/${jar_name}" ]]; then
  echo "--skip-build requires target/${jar_name} to exist and be non-empty." >&2
  exit 1
fi

mkdir -p "${input_dir}" "${package_output_dir}" "${output_dir}"
cp "${project_dir}/target/${jar_name}" "${input_dir}/${jar_name}"

jpackage_args=(
  --type dmg
  --name Invoicely
  --app-version "${version}"
  --vendor "Eduard Tomas"
  --description "Offline invoices and quotes, without the fuss."
  --input "${input_dir}"
  --main-jar "${jar_name}"
  --main-class app.invocely.InvoicelyApp
  --add-modules "java.base,java.compiler,java.desktop,java.prefs,java.sql.rowset,jdk.xml.dom,jdk.unsupported,jdk.localedata"
  --java-options "--enable-native-access=ALL-UNNAMED"
  --icon "${project_dir}/src/main/resources/icon/invocely.icns"
  --resource-dir "${project_dir}/packaging/macos"
  --mac-package-identifier app.invocely.desktop
  --mac-app-category business
  --dest "${package_output_dir}"
)

if [[ -n "${signing_identity}" ]]; then
  if [[ "${signing_identity}" != "Developer ID Application:"* ]]; then
    echo "INVOCELY_MAC_SIGNING_IDENTITY must name a Developer ID Application identity." >&2
    exit 1
  fi
  signing_user_name="${signing_identity#Developer ID Application: }"
  jpackage_args+=(
    --mac-sign
    --mac-package-signing-prefix app.invocely.desktop.
    --mac-signing-key-user-name "${signing_user_name}"
  )
  if [[ -n "${signing_keychain}" ]]; then
    jpackage_args+=(--mac-signing-keychain "${signing_keychain}")
  fi
  echo "Creating a Developer ID-signed macOS package."
elif [[ -n "${signing_keychain}" ]]; then
  echo "INVOCELY_MAC_SIGNING_KEYCHAIN requires INVOCELY_MAC_SIGNING_IDENTITY." >&2
  exit 1
else
  echo "Creating a local development DMG without Developer ID signing or notarization." >&2
  echo "This artifact is not a trusted release." >&2
fi

jpackage "${jpackage_args[@]}"

generated_dmg="${package_output_dir}/Invoicely-${version}.dmg"
final_dmg="${output_dir}/Invoicely-${version}-macos-${artifact_arch}.dmg"
mv -f "${generated_dmg}" "${final_dmg}"

echo "Created ${final_dmg}"
