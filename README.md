# RealWorld

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

## Either<E, T>とValidated<E, T>

|型   |説明                                |
|:---:|:----------------------------------|
|E    |自作エラー(例: NotFoundRegisteredUser)|
|T    |DomainObject                       |

## 他レイヤーやテスト方法等

[詳細](./docs/README.md)
