/*
 * CommitLintの設定ファイル
 */
module.exports = {
  extends: [
    /*
     * Conventional Commits
     *
     * ルールの内容
     * - https://www.conventionalcommits.org/ja/v1.0.0/
     *
     * リポジトリ
     * - https://github.com/conventional-changelog/commitlint/tree/master/%40commitlint/config-conventional
     */
    '@commitlint/config-conventional'
  ],
  /*
   * helpUrl
   *
   * lintに引っかかった時に見せるURL
   */
  helpUrl: 'https://www.conventionalcommits.org/ja/v1.0.0/'
}
