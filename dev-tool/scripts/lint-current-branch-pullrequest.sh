#!/bin/bash

#
# Lint pull request
#
# 概要
# - pull request のタイトルに 2 種類の linter をかけます
#   - commitlint
#   - textlint
# - pull request の内容に 1 種類の linter をかけます
#   - textlint
#
# 事前に必要
# ```shell
# npm install
# ```
#
# local上での使い方
# ```shell
# bash lint-current-branch-pullrequest.sh
# ```
#

readonly pr_title="${PR_TITLE:-$(gh pr view --json 'title' | jq -r '.title')}"
readonly pr_body="${PR_BODY:-$(gh pr view --json 'body' | jq -r '.body' | sed 's/\r//g')}"
readonly WORKING_DIR_PATH=$(pwd)
readonly config_file_path="$WORKING_DIR_PATH/.textlintrc.for-git-and-github.yml"
mkdir -p tmp

echo '****************************************************'
echo '* commitlint PR title'
echo '****************************************************'

echo "$pr_title" | npx commitlint
readonly pr_title_commitlint_exit_code=$?

echo ''
echo '****************************************************'
echo '* textlint PR title'
echo '****************************************************'

echo "$pr_title" > tmp/PULL_REQUEST_TITLE.md
npx textlint --config "$config_file_path" tmp/PULL_REQUEST_TITLE.md
readonly pr_title_textlint_exit_code=$?

echo ''
echo '****************************************************'
echo '* textlint PR body'
echo '****************************************************'

echo "$pr_body" > tmp/PULL_REQUEST_BODY.md
npx textlint --config "$config_file_path" tmp/PULL_REQUEST_BODY.md
readonly pr_body_textlint_exit_code=$?

echo ''
echo '****************************************************'
echo '* Result'
echo '****************************************************'

if [ $pr_title_commitlint_exit_code -eq 0 ]; then
  echo '✅ passed: commitlint for pr title'
else
  echo '👺 commitlint は通りませんでした'
  echo '👺 help url を参考に PR title を編集し直してください ( 修正が終われば job の rerun )'
fi

if [ $pr_title_textlint_exit_code -eq 0 ]; then
  echo '✅ passed: textlint for pr title'
else
  echo ''
  echo '👺 pr title に対して、 textlint は通りませんでした'
  echo '👺 指摘箇所を修正し直してください ( 修正が終われば job の rerun )'
  echo 'Try to run on local:'
  echo 'make lint.current-branch-pullrequest'
  echo 'cp tmp/PULL_REQUEST_TITLE.md tmp/PULL_REQUEST_TITLE.md.bk'
  echo 'npx textlint --fix --config "$config_file_path" tmp/PULL_REQUEST_TITLE.md'
  echo 'diff -u tmp/PULL_REQUEST_TITLE.md.bk tmp/PULL_REQUEST_TITLE.md | delta'
fi

if [ $pr_body_textlint_exit_code -eq 0 ]; then
  echo '✅ passed: textlint for pr body'
else
  echo ''
  echo '👺 pr body に対して、 textlint は通りませんでした'
  echo '👺 指摘箇所を修正し直してください ( 修正が終われば job の rerun )'
  echo 'Try to run on local:'
  echo 'make lint.current-branch-pullrequest'
  echo 'cp tmp/PULL_REQUEST_BODY.md tmp/PULL_REQUEST_BODY.md.bk'
  echo 'npx textlint --fix --config "$config_file_path" tmp/PULL_REQUEST_BODY.md'
  echo 'diff -u tmp/PULL_REQUEST_BODY.md.bk tmp/PULL_REQUEST_BODY.md | delta'
fi

[[ $pr_title_commitlint_exit_code = 0 ]] && [[ $pr_title_textlint_exit_code = 0 ]] && [[ $pr_body_textlint_exit_code = 0 ]]
