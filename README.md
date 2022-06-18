# RealWorld

## アーキテクチャ

![](./docs/architecture.png)

- Service層は必ずEither<サービスエラー, ドメインオブジェクト>を返す
- Repository層は必ずEither<サービスエラー, ドメインオブジェクト>を返す
- Repository層はドメインオブジェクトのバリデーションなしでインスタンス化可能である
- 基本的にService層はドメインオブジェクトをインスタンス化する時、バリデーションをかける