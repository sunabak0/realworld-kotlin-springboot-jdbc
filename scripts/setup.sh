#!/bin/bash

set -eu

################################################################################
# git commit message template
################################################################################
function setupGitCommitMessageTemplate() {
  echo '🚀 Setup git commit message template'
  echo '---------------------------------------------------'

  git config --local commit.template .GIT_COMMIT_MESSAGE_TEMPLATE.md

  echo '✅ Done: setup git commit message template'
  echo ''
}

################################################################################
# npm 依存の解決 ( textlint 等が入る )
################################################################################
function resolveNpmDependencies() {
  echo '🚀 Resolve npm dependencies'
  echo '---------------------------------------------------'

  npm install

  echo '✅ Done: resolve npm dependencies'
  echo ''
}

################################################################################
# githooks
################################################################################
function setupGitHooks() {
  echo '🚀 Setup githooks'
  echo '---------------------------------------------------'

  git config --local core.hooksPath .githooks

  echo '✅ Done: setup githooks'
  echo ''
}

################################################################################
# main
################################################################################
function main() {
  echo ''

  setupGitCommitMessageTemplate
  resolveNpmDependencies
  setupGitHooks
}

cat << COMMAND_BEGIN
################################################################################
# 🛠️ 開発環境をセットアップします
################################################################################
COMMAND_BEGIN

main

echo '✅ Done: all setup'
