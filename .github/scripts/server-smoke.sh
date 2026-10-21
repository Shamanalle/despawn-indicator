#!/usr/bin/env bash
# Starts a real Minecraft server with Despawn Indicator, runs /despawnindicator from the console and
# stops it. Fails when the server does not come up, when the command answers with bare text keys, or
# when anything of the mod throws.
#
#   server-smoke.sh <fabric|neoforge|forge> <minecraft version> <mod jar> [loader version]
#
# The loader version is needed for NeoForge and Forge: the installer's version (Forge and NeoForge for
# 1.20.1: "1.20.1-47.4.0"). Everything else is looked up: Fabric's loader and installer from
# meta.fabricmc.net, Fabric API from Modrinth.
#
# A download that fails on the way (net-failure.sh: a timeout, a reset connection, HTTP 429 / 5xx) is
# tried again: curl does it by itself, the Forge / NeoForge installer runs once more, and the server is
# started once more when it stopped on a download before any mod was loaded. Each of these leaves a
# warning on the run. Everything after that is checked once.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=net-failure.sh
. "${here}/net-failure.sh"

loader="$1"
mc="$2"
mod="$(realpath "$3")"
loader_version="${4:-}"
dir="${RUNNER_TEMP:-/tmp}/server-${loader}-${mc}"
log="${dir}/server.log"
start_timeout=300

rm -rf "${dir}"
mkdir -p "${dir}"
cd "${dir}"

say() {
  echo "::group::$*" >&2
  echo "::endgroup::" >&2
  echo "== $*"
}

# curl tries a timeout, a refused connection and HTTP 408 / 429 / 5xx again by itself; a 404 is final
api() {
  curl -fsSL --retry 4 --retry-delay 3 --retry-connrefused --connect-timeout 20 --max-time 120 "$@"
}

fetch() {
  curl -fsSL --retry 4 --retry-delay 3 --retry-connrefused --connect-timeout 20 --max-time 900 -o "$2" "$1"
}

# A warning on the run, so a passing job still shows that something had to be tried twice
retrying() {
  echo "::warning title=Network failure (${loader} ${mc})::$*"
  sleep "${NET_RETRY_DELAY:-30}"
}

# The newest file of a Modrinth project for this loader and Minecraft version. A file built for exactly
# this version ("forge-1.21-2.5.20") comes first: one listed for 1.21 - 1.21.1 may need 1.21.1.
modrinth() {
  local slug="$1" loaders="$2" out="$3"
  local url
  url=$(api -G "https://api.modrinth.com/v2/project/${slug}/version" \
      --data-urlencode "loaders=${loaders}" --data-urlencode "game_versions=[\"${mc}\"]" \
    | jq -r --arg mc "${mc}" '[.[] | select(.version_type == "release" or .version_type == "beta")]
             | ([.[] | select(.version_number | test("(^|[^0-9.])" + ($mc | gsub("\\."; "\\.")) + "-[0-9]"))] + .)[0].files
             | (map(select(.primary)) + .)[0].url // empty')
  if [ -z "${url}" ]; then
    echo "No ${slug} on Modrinth for ${loaders} ${mc}"
    return 1
  fi
  echo "${slug}: ${url}"
  fetch "${url}" "${out}"
}

say "Setting up ${loader} ${mc}"
case "${loader}" in
  fabric)
    fabric_loader=$(api https://meta.fabricmc.net/v2/versions/loader | jq -r '[.[] | select(.stable)][0].version')
    installer=$(api https://meta.fabricmc.net/v2/versions/installer | jq -r '[.[] | select(.stable)][0].version')
    fetch "https://meta.fabricmc.net/v2/versions/loader/${mc}/${fabric_loader}/${installer}/server/jar" server.jar
    mkdir -p mods
    modrinth fabric-api '["fabric"]' mods/fabric-api.jar || exit 1
    cp "${mod}" mods/
    run=(java -Xmx2G -jar server.jar nogui)
    ;;
  neoforge | forge)
    if [ "${loader}" = "neoforge" ] && [ "${loader_version#1.20.1-}" != "${loader_version}" ]; then
      # NeoForge for 1.20.1 is still published as Forge ("1.20.1-47.1.106")
      fetch "https://maven.neoforged.net/releases/net/neoforged/forge/${loader_version}/forge-${loader_version}-installer.jar" installer.jar
    elif [ "${loader}" = "neoforge" ]; then
      fetch "https://maven.neoforged.net/releases/net/neoforged/neoforge/${loader_version}/neoforge-${loader_version}-installer.jar" installer.jar
    else
      fetch "https://maven.minecraftforge.net/net/minecraftforge/forge/${loader_version}/forge-${loader_version}-installer.jar" installer.jar
    fi
    # The installer downloads Minecraft and the loader's libraries; run again, it keeps the files that
    # came whole (it checks each one's checksum) and fetches the rest
    if ! java -jar installer.jar --installServer > installer.log 2>&1; then
      if ! network_failure installer.log; then
        tail -40 installer.log
        exit 1
      fi
      mv installer.log installer-1.log
      retrying "the installer could not download everything ($(grep -Eo "${net_failure_pattern}" installer-1.log | head -1)), running it once more"
      java -jar installer.jar --installServer > installer.log 2>&1 || { tail -40 installer.log; exit 1; }
    fi
    mkdir -p mods
    cp "${mod}" mods/
    echo "-Xmx2G" > user_jvm_args.txt
    run=(bash ./run.sh nogui)
    ;;
  *)
    echo "Unknown loader ${loader}"
    exit 2
    ;;
esac

echo "eula=true" > eula.txt
cat > server.properties <<'EOF'
online-mode=false
level-type=minecraft\:flat
generate-structures=false
spawn-protection=0
max-players=4
view-distance=4
simulation-distance=4
server-port=25599
EOF

send() {
  echo "$*" >&3
}

stop_server() {
  if kill -0 "${server}" 2>/dev/null; then
    send stop
    for _ in $(seq 1 60); do
      kill -0 "${server}" 2>/dev/null || break
      sleep 1
    done
    kill -9 "${server}" 2>/dev/null || true
  fi
}

# Starts the server and waits for it. Sets state to "up", "stopped" (the process ended first) or
# "timeout". The console reads from a FIFO that stays open, so commands can be sent while it runs.
start_server() {
  rm -f console
  mkfifo console
  exec 3<>console
  "${run[@]}" < console > "${log}" 2>&1 &
  server=$!
  for _ in $(seq 1 "${start_timeout}"); do
    if grep -q 'Done (' "${log}"; then
      state=up
      return
    fi
    if ! kill -0 "${server}" 2>/dev/null; then
      state=stopped
      return
    fi
    sleep 1
  done
  state=timeout
}

say "Waiting for the server"
server=''
trap stop_server EXIT
start_server
# Fabric's launcher and Paper download Minecraft and its libraries on the first start, before any mod
# or plugin is loaded. Only a server that stopped there, on a failed download, is started again.
if [ "${state}" = stopped ] && network_failure "${log}" \
    && ! grep -Eiq 'despawn.?indicator' "${log}"; then
  exec 3>&-
  mv "${log}" "${dir}/server-1.log"
  retrying "${loader} ${mc} stopped on a download before loading any mod ($(grep -Eo "${net_failure_pattern}" "${dir}/server-1.log" | head -1)), starting it once more"
  start_server
fi
if [ "${state}" != up ]; then
  tail -80 "${log}"
  echo "::error::${loader} ${mc} did not start"
  exit 1
fi

say "Running /despawnindicator"
send "despawnindicator"
sleep 3
stop_server
trap - EXIT

# Some consoles colour the log with ANSI codes, which split the answers below
sed -i 's/\x1b\[[0-9;]*m//g' "${log}"

failed=false
for answer in 'Despawn Indicator [0-9]' 'Players with the mod: 0 of 0 online' 'Dropped items despawn after 5:00 here'; do
  if ! grep -Eq "${answer}" "${log}"; then
    echo "::error::/despawnindicator did not answer with \"${answer}\""
    failed=true
  fi
done
if grep -q 'command\.despawnindicator\.' "${log}"; then
  echo "::error::/despawnindicator answered with text keys instead of messages"
  failed=true
fi
if grep -q 'at com\.kasper\.despawnindicator' "${log}"; then
  echo "::error::The mod threw an exception"
  grep -n -B8 -A4 'at com\.kasper\.despawnindicator' "${log}" | head -80
  failed=true
fi
# The universal Forge / NeoForge jar has one pack.mcmeta for every Minecraft it supports
if grep -Ei 'despawnindicator|despawn-indicator' "${log}" | grep -Ei 'pack|mcmeta|metadata|resources' \
    | grep -Eiq 'fail|error|invalid|missing|couldn|unable|broken'; then
  echo "::error::The mod's resources or pack.mcmeta were refused"
  grep -Ei 'despawnindicator|despawn-indicator' "${log}" | grep -Ei 'pack|mcmeta|metadata|resources' | head -20
  failed=true
fi

say "Server log"
grep -Ei 'despawn ?indicator|exception|error' "${log}" | head -120 || true

if [ "${failed}" = true ]; then
  echo "--- last lines ---"
  tail -60 "${log}"
  exit 1
fi
echo "${loader} ${mc}: OK"
