#!/bin/bash

#
# Lint commit messages
#
# 概要
# - git commit message に 2 種類の linter をかけます
#   - commitlint
#   - textlint
#
# 事前に必要
# ```shell
# npm install
# ```
#
# local上での使い方
# ```shell
# bash lint-commit-messages.sh
# ```
#
# CI上では以下の環境変数にそれぞれ注入します
#
# commitlint用
# - FROM_COMMIT_ID: default は local の main branch の最新の commit_id
# - TO_COMMIT_ID: default は local の current branch の最新の commit_id
#
# textlint用
# - CURRENT_BRANCH_NAME: default は local の current branch 名
#

readonly WORKING_DIR_PATH=$(pwd)
readonly BASE_COMMIT_ID="$(git log -n 1 --pretty=%H main)"
readonly CURRENT_HEAD_COMMIT_ID="$(git log -n 1 --pretty=%H)"
readonly from_commit_id="${FROM_COMMIT_ID:-${BASE_COMMIT_ID}}"
readonly to_commit_id="${TO_COMMIT_ID:-${CURRENT_HEAD_COMMIT_ID}}"
readonly current_branch_name=${CURRENT_BRANCH_NAME:-$(git rev-parse --abbrev-ref HEAD | sed 's/\//--/g')}

echo '****************************************************'
echo '* commitlint'
echo '****************************************************'

npx commitlint --from "$from_commit_id" --to "$to_commit_id"
readonly commitlint_exit_code=$?

echo ''
echo '****************************************************'
echo '* textlint commit messages'
echo '****************************************************'
readonly config_file_path="$WORKING_DIR_PATH/.textlintrc.for-git-and-github.yml"

rm -rf "tmp/BRANCH_$current_branch_name"
mkdir -p "tmp/BRANCH_$current_branch_name"
while read commit_id; do
  git log -n 1 "$commit_id" --pretty=%B > "tmp/BRANCH_$current_branch_name/COMMIT_$(git log -n 1 "$commit_id" --pretty=format:'%cd_%h' --date=format:'%Y-%m-%dT%H:%M:%S').md"
done < <(git rev-list $BASE_COMMIT_ID..)
npx textlint --config "$config_file_path" "tmp/BRANCH_$current_branch_name/*.md"
readonly textlint_exit_code=$?

echo ''
echo '****************************************************'
echo '* Result'
echo '****************************************************'

if [ $commitlint_exit_code -eq 0 ]; then
  echo '✅ passed: commitlint'
else
  echo '👺 commitlint は通りませんでした'
  echo '👺 help url を参考に commit message を編集し直してください'
  echo "e.g. \`git commit --amend\` or \`git rebase -i $BASE_COMMIT_ID\`"
fi

if [ $textlint_exit_code -eq 0 ]; then
  echo '✅ passed: textlint'
else
  echo ''
  echo '👺 textlint は通りませんでした'
  echo '👺 指摘箇所を修正し直してください'
  echo "Try to run:"
  echo "cp tmp/BRANCH_$current_branch_name/ tmp/BRANCH_$current_branch_name.bk/"
  echo "npx textlint --config $config_file_path tmp/BRANCH_$current_branch_name/*.md"
  echo "diff -ur tmp/BRANCH_$current_branch_name.bk/ tmp/BRANCH_$current_branch_name.bk/ | delta"
fi

[[ $commitlint_exit_code = 0 ]] && [[ $textlint_exit_code = 0 ]]
