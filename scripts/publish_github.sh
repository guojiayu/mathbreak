#!/usr/bin/env bash
# Creates a NEW GitHub repository and pushes this project's main branch.
# Requires local GitHub CLI (`gh auth login`) and git; default is private.
set -euo pipefail
REPO_NAME="${1:-mathbreak-tv}"
VISIBILITY="${2:---private}"
case "$VISIBILITY" in --public|--private) ;; *) echo 'Visibility must be --public or --private' >&2; exit 2;; esac
cd "$(dirname "${BASH_SOURCE[0]}")/.."
command -v gh >/dev/null || { echo 'Install GitHub CLI: https://cli.github.com/' >&2; exit 1; }
command -v git >/dev/null || { echo 'git is required' >&2; exit 1; }
gh auth status >/dev/null
if [ ! -d .git ]; then
  git init -b main
  git add .
  git commit -m 'Initial MathBreak TV Android MVP'
fi
# gh prompts / fails rather than silently overwriting an existing repository.
gh repo create "$REPO_NAME" "$VISIBILITY" --source=. --remote=origin --push
