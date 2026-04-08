#!/usr/bin/env bash
set -euo pipefail

VERSION_CHANNEL="${VERSION_CHANNEL:-stable}"
MC_VERSION_OVERRIDE="${MC_VERSION_OVERRIDE:-}"
GAME_VERSIONS=$(curl -fsSL "https://meta.fabricmc.net/v2/versions/game")
MANIFEST=$(curl -fsSL https://launchermeta.mojang.com/mc/game/version_manifest_v2.json)

if [[ -n "$MC_VERSION_OVERRIDE" ]]; then
  MC_VERSION="$MC_VERSION_OVERRIDE"
elif [[ "$VERSION_CHANNEL" == "snapshot" ]]; then
  MC_VERSION=$(echo "$GAME_VERSIONS" | jq -r '[.[] | select(.stable == false)] | .[0].version')
else
  MC_VERSION=$(echo "$GAME_VERSIONS" | jq -r '[.[] | select(.stable == true)] | .[0].version')
fi

MC_META_URL=$(echo "$MANIFEST" | jq -r --arg v "$MC_VERSION" '.versions[] | select(.id == $v) | .url')
JAVA_VERSION=$(curl -fsSL "$MC_META_URL" | jq -r '.javaVersion.majorVersion')
LOADER_VERSION=$(curl -fsSL "https://meta.fabricmc.net/v2/versions/loader" | jq -r '[.[] | select(.stable == true)] | .[0].version')
FABRIC_VERSION=$(curl -fsSL "https://api.modrinth.com/v2/project/fabric-api/version?game_versions=%5B%22${MC_VERSION}%22%5D&loaders=%5B%22fabric%22%5D" | jq -r '.[0].version_number')
LOOM_VERSION=$(curl -fsSL "https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml" | grep -o '<release>[^<]*</release>' | sed 's/<[^>]*>//g')

for var in MC_VERSION JAVA_VERSION LOADER_VERSION FABRIC_VERSION LOOM_VERSION; do
  val="${!var}"
  if [ -z "$val" ] || [ "$val" = "null" ]; then
    echo "ERROR: $var is empty or null" >&2
    exit 1
  fi
done

echo "MC_VERSION=${MC_VERSION}"
echo "JAVA_VERSION=${JAVA_VERSION}"
echo "LOADER_VERSION=${LOADER_VERSION}"
echo "FABRIC_VERSION=${FABRIC_VERSION}"
echo "LOOM_VERSION=${LOOM_VERSION}"

{
  echo "### Minecraft ${MC_VERSION}"
  echo "- Java: ${JAVA_VERSION}"
  echo "- Loader: ${LOADER_VERSION}"
  echo "- Fabric API: ${FABRIC_VERSION}"
  echo "- Fabric Loom: ${LOOM_VERSION}"
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

{
  echo "MC_VERSION=${MC_VERSION}"
  echo "JAVA_VERSION=${JAVA_VERSION}"
} >> "${GITHUB_ENV:-/dev/null}"

# sed -i behaves differently on macOS vs Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
  SED_INPLACE=(-i '')
else
  SED_INPLACE=(-i)
fi

sed "${SED_INPLACE[@]}" "s|^minecraft_version=.*|minecraft_version=${MC_VERSION}|" gradle.properties
sed "${SED_INPLACE[@]}" "s|^loader_version=.*|loader_version=${LOADER_VERSION}|" gradle.properties
sed "${SED_INPLACE[@]}" "s|^fabric_version=.*|fabric_version=${FABRIC_VERSION}|" gradle.properties
sed "${SED_INPLACE[@]}" "s|id 'net.fabricmc.fabric-loom' version '.*'|id 'net.fabricmc.fabric-loom' version '${LOOM_VERSION}'|" build.gradle
