/*
 * textlint の設定ファイル
 */
const IS_FOR_GIT = !!process.env.TEXTLINT_FOR_GIT;

module.exports = {
  filters: {
    /*
     * コメントでtextlintのルールを一部disableできるようにする
     *
     * https://github.com/textlint/textlint-filter-rule-comments
     *
     * <!-- textlint-disable xxx --> と <!-- textlint-enable xxx --> で囲う
     */
    comments: true,
  },

  rules: {
    /*
     * 技術文書向けのtextlintルールプリセット郡
     *
     * https://github.com/textlint-ja/textlint-rule-preset-ja-technical-writing
     *
     * IS_FOR_GIT でのみ句点 CHECK を OFF
     */
    "preset-ja-technical-writing": IS_FOR_GIT
      ? {
        "ja-no-mixed-period": false // 末尾句点
      }
      : true,

    /*
     * リンクの周りを半角スペースで囲むかどうか(default: 両方false)
     *
     * https://github.com/textlint-ja/textlint-rule-preset-ja-spacing/tree/master/packages/textlint-rule-ja-space-around-link
     */
    "ja-space-around-link": {
      before: true,
      after: true,
    },

    /*
     * 日本語周りにおけるスペースの有無を決定するtextlintルールプリセット郡
     *
     * https://github.com/textlint-ja/textlint-rule-preset-ja-spacing
     */
    "preset-ja-spacing": {
      /*
       * インラインコードの周りをスペースで囲むかどうか(default: 両方false)
       */
      "ja-space-around-code": {
        before: true,
        after: true
      },
      /*
       * 半角文字と全角文字にスペースを入れるかどうか(default: "never", 入れない)
       */
      "ja-space-between-half-and-full-width": {
        space: "always"
      },
    },
  },
};
