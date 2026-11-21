#!/bin/sh
# @file 在应用构建前同步 Mui 子模块，并按需安装 Web 依赖。
set -eu

fail() {
  printf '[Mui] %s\n' "$*" >&2
  exit 1
}

case "${1:-}" in
  web|android) platform=$1 ;;
  *) fail "Usage: sync-mui.sh web|android" ;;
esac

muiPath=libs/mui
cd "$(git rev-parse --show-toplevel)"
lockPath=$(git rev-parse --path-format=absolute --git-path mui-sync.lock)

# @brief 串行执行同步和安装，避免并发修改同一个子模块。
waitCount=0
until mkdir "$lockPath" 2>/dev/null; do
  [ -d "$lockPath" ] || fail "Cannot create lock: $lockPath"
  [ "$waitCount" -lt 60 ] || fail "Timed out waiting for $lockPath"
  sleep 1
  waitCount=$((waitCount + 1))
done
trap 'rmdir "$lockPath"' EXIT
trap 'exit 1' HUP INT TERM

gitLink=$(git ls-files --stage -- "$muiPath")
case "$gitLink" in
  "160000 "*) ;;
  *) fail "Register $muiPath as a Git submodule before building." ;;
esac

if [ ! -e "$muiPath/.git" ]; then
  git submodule update --quiet --init --checkout -- "$muiPath" >&2
fi

localChanges=$(git -C "$muiPath" status --porcelain --untracked-files=normal)
[ -z "$localChanges" ] || fail "$muiPath has local changes. Save them before syncing."
currentBranch=$(git -C "$muiPath" branch --show-current)
[ -z "$currentBranch" ] || fail "$muiPath is on branch $currentBranch. Detach HEAD before syncing."

currentCommit=$(git -C "$muiPath" rev-parse HEAD)
git -C "$muiPath" fetch --quiet --no-tags origin '+refs/heads/main:refs/remotes/origin/main' >&2
remoteCommit=$(git -C "$muiPath" rev-parse refs/remotes/origin/main)
if [ "$currentCommit" != "$remoteCommit" ]; then
  git -C "$muiPath" merge-base --is-ancestor "$currentCommit" "$remoteCommit" ||
    fail "$currentCommit cannot fast-forward to origin/main ($remoteCommit)."
  git -C "$muiPath" checkout --quiet --detach "$remoteCommit" >&2
fi

if [ "$platform" = web ]; then
  webPath="$muiPath/web"
  installStamp="$webPath/node_modules/.mui-install"
  runtime=$(node -p '[process.platform, process.arch, process.versions.modules].join("/")')
  manifestHash=$(git hash-object "$webPath/package.json" "$webPath/package-lock.json")
  if [ -f "$webPath/.npmrc" ]; then
    manifestHash="$manifestHash $(git hash-object "$webPath/.npmrc")"
  fi
  installKey="$runtime $manifestHash"
  if [ ! -f "$installStamp" ] || [ ! -f "$webPath/node_modules/.package-lock.json" ] ||
     [ "$(cat "$installStamp")" != "$installKey" ]; then
    npm ci --prefix "$webPath" --include=dev --prefer-offline --no-audit --no-fund >&2
    printf '%s\n' "$installKey" > "$installStamp"
  fi
fi

printf '[Mui] %s\n' "$remoteCommit" >&2
printf '%s\n' "$remoteCommit"
