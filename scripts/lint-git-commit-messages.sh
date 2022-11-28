#!/bin/bash

################################################################################
# Lint git commit messages
################################################################################
#
# 概要
# - git commit message に対して lint
#
# CI時のDI対象
#
# - FROM_COMMIT_ID: lint 対象の commit message の from
# - TO_COMMIT_ID: lint 対象の commit message の to
# - CURRENT_BRANCH_NAME: commit message を置くディレクトリ名に利用
#

WORKING_DIR_PATH="$(pwd)"
LOCAL_MAIN_BRANCH_LATEST_COMMIT_ID="$(git log -n 1 --pretty=%H main)"
CURRENT_BRANCH_LATEST_COMMIT_ID="$(git log -n 1 --pretty=%H)"
readonly WORKING_DIR_PATH
readonly LOCAL_MAIN_BRANCH_LATEST_COMMIT_ID
readonly CURRENT_BRANCH_LATEST_COMMIT_ID

# ブランチ名の '/' は '--' に置換
readonly current_branch_name=${CURRENT_BRANCH_NAME:-$(git rev-parse --abbrev-ref HEAD | sed 's/\//--/g')}
readonly from_commit_id="${FROM_COMMIT_ID:-${LOCAL_MAIN_BRANCH_LATEST_COMMIT_ID}}"
readonly to_commit_id="${TO_COMMIT_ID:-${CURRENT_BRANCH_LATEST_COMMIT_ID}}"

################################################################################
# commitlint
################################################################################
function commitlint() {
  echo ''
  echo '👮 Lint: commitlint'
  echo '---------------------------------------------------'

  npx commitlint --from "$from_commit_id" --to "$to_commit_id"
}

################################################################################
# textlint
################################################################################
function textlint() {
  echo ''
  echo '👮 Lint: textlint'
  echo '---------------------------------------------------'

  readonly tmp_branch_dir_path="${WORKING_DIR_PATH}/tmp/BRANCH_${current_branch_name}"
  rm -rf "$tmp_branch_dir_path"
  mkdir -p "$tmp_branch_dir_path"
  while read -r commit_id; do
    git log -n 1 "$commit_id" --pretty=%B > "${tmp_branch_dir_path}/COMMIT_$(git log -n 1 "$commit_id" --pretty=format:'%cd_%h' --date=format:'%Y-%m-%dT%H:%M:%S').md"
  done < <(git rev-list "${from_commit_id}"..)

  TEXTLINT_FOR_GIT="true" npx textlint --config "${WORKING_DIR_PATH}/.textlintrc.js" "${tmp_branch_dir_path}/*.md"
}

################################################################################
# Report result
################################################################################
function reportResult() {
  echo ''
  echo '📝 Result: lint git commit messages'
  echo '---------------------------------------------------'

  if [ "$commitlint_exit_code" -eq 0 ]; then
    echo '✅ Passed: commitlint'
  else
    echo '👺 Failed: commitlint'
  fi
  if [ "$textlint_exit_code" -eq 0 ]; then
    echo '✅ Passed: textlint'
  else
    echo '👺 Failed: textlint'
  fi


  if [ "$commitlint_exit_code" -ne 0 ]; then
    echo ''
    echo '[commitlint]'
    echo '👺 help url を参考に commit message を修正してください'
    echo "Try to run: \`git commit --amend\` or \`git rebase -i $LOCAL_MAIN_BRANCH_LATEST_COMMIT_ID\`"
  fi
  if [ "$textlint_exit_code" -ne 0 ]; then
    echo ''
    echo '[textlint]'
    echo '👺 指摘された commit message を修正してください'
    echo 'Try to run: 以下のコマンドを試して diff をとって参考にしてください'
    echo '```'
    echo "cp tmp/BRANCH_$current_branch_name/ tmp/BRANCH_$current_branch_name.bk/"
    echo "TEXTLINT_FOR_GIT=\"true\" npx textlint --config \"${WORKING_DIR_PATH}/.textlintrc.js\" \"${tmp_branch_dir_path}/*.md\""
    echo "diff -ur tmp/BRANCH_$current_branch_name.bk/ tmp/BRANCH_$current_branch_name.bk/ | delta"
    echo '```'
  fi
}

################################################################################
# Main
################################################################################
function main() {
  commitlint
  readonly commitlint_exit_code=$?
  textlint
  readonly textlint_exit_code=$?

  reportResult
  [[ "$commitlint_exit_code" = 0 ]] && [[ "$textlint_exit_code" = 0 ]]
}

cat << COMMAND_BEGIN
################################################################################
# 👮 Lint git commit messages
################################################################################
CURRENT BRANCH NAME: $current_branch_name
FROMT COMMIT ID: $from_commit_id
TO COMMIT ID:    $to_commit_id
---------------------------------------------------
COMMAND_BEGIN

if [ "$current_branch_name" = 'main' ]; then
  echo '現在のブランチが "main" なので、 lint を skip します'
  exit 0
fi

main
