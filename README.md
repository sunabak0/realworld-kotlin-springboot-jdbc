# RealWorld

![ci-status](https://github.com/sunakan/realworld-kotlin-springboot-jdbc/actions/workflows/ci.yml/badge.svg)
[![Maintainability](https://api.codeclimate.com/v1/badges/de2d7acc6b617132951a/maintainability)](https://codeclimate.com/github/sunakan/realworld-kotlin-springboot-jdbc/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/de2d7acc6b617132951a/test_coverage)](https://codeclimate.com/github/sunakan/realworld-kotlin-springboot-jdbc/test_coverage)

## 開発

```
make
```

```
######################################################################
# Makeタスク一覧
# $ make XXX
# or
# $ make XXX --dry-run
######################################################################
up                   サーバー起動
up.db                db起動
down.db              dbを落とす
test                 テスト実行
test.with-local-db   テスト(with local db)実行
test.e2e             e2eテスト実行
fmt                  format and lint
help                 Make タスク一覧
```

## アーキテクチャ

![](./docs/architecture-basic.drawio.png)

## UseCase

- UseCase層は必ず `Either<UseCaseError, DomainObject>` を返す
  - DomainErrorをそのまま返さないこと（必要であればWrapする）
    - Wrapする時、Either自体の入れ子はやめること(取り出して中身をWrapする)
- 基本的にUseCase層はDomainObjectをインスタンス化する時、バリデーションをかける
- UseCaseErrorの命名では技術的用語を使わないようにする

## Infra

- Infra層は必ずEither<DomainError, DomainObject>を返す
- Infra層はDobmainObjectのバリデーション無しでインスタンス化可能である

## Either<E, T>とValidatedNel<E, T>

|型   |説明                                |
|:---:|:----------------------------------|
|E    |自作エラー(例: NotFoundRegisteredUser)|
|T    |DomainObject                       |

## 他レイヤーやテスト方法等

[詳細](./docs/README.md)
