.PHONY: up
up: ## サーバー起動
	./gradlew bootRun

.PHONY: up.db
up.db: ## db起動
	docker compose up

.PHONY: db.dump
db.dump: ## 現在のdbの状態をseedとしてdump
	docker compose exec realworld-pg bash -c 'pg_dump realworld-db --inserts -a -Urealworld-user > /docker-entrypoint-initdb.d/002-realworld-seed.sql'

.PHONY: down.db
down.db: ## dbを落とす
	docker compose down

.PHONY: test
test: ## テスト実行
	@make test.clean
	./gradlew test unitWithoutDb

.PHONY: test.full
test.full: ## db有りで全てのテスト実行(API/DBUnit含む、sandboxは除く)
	@make test.clean
	docker compose up -d --wait
	./gradlew test full

.PHONY: test.full-dev
test.full-dev: ## db有りで全てのテスト実行(API/DBUnit/sandbox含む)
	@make test.clean
	docker compose up -d --wait
	./gradlew test fullDev

.PHONY: test.integration
test.integration: ## db有りでAPIテスト実行
	@make test.clean
	docker compose up -d --wait
	./gradlew test apiIntegration

.PHONY: test.clean
test.clean: ## テストレポート類を削除
	rm -rf build/jacoco/ build/reports/

.PHONY: test.e2e
test.e2e: ## e2eテスト実行
	bash e2e/run-api-tests.sh

.PHONY: fmt
fmt: ## format
	./gradlew detekt --auto-correct

.PHONY: lint
lint: ## lint
	./gradlew detekt

.PHONY: lint.for-yaml
lint.for-yaml: ## lint for yaml
	docker run --rm -it --mount type=bind,source=${PWD}/,target=/code/ pipelinecomponents/yamllint yamllint .

.PHONY: lint.for-commit-message
lint.for-commit-message: ## lint for commit message(必須: npm install)
	@npx commitlint --from $$(git log -n 1 --pretty=%H main) --to $$(git log -n 1 --pretty=%H) && echo '🎉Good commit messages🎉'

.PHONY: lint.for-github-action
lint.for-github-action: ## lint for github action
	docker run --rm --mount type=bind,source=${PWD}/,target=/repo --workdir /repo rhysd/actionlint:latest -color

.PHONY: docs.generate-db-docs-schemaspy
docs.generate-db-docs-schemaspy: ## schemaspyでDB用のドキュメントを作成、表示する(gitに含めない)
	mkdir -p ./tmp/db-drivers/
	ls ./tmp/db-drivers/postgresql-42.4.0.jar || curl -o ./tmp/db-drivers/postgresql-42.4.0.jar https://jdbc.postgresql.org/download/postgresql-42.4.0.jar
	mkdir -p ./tmp/schemaspy-output/
	docker run --rm -it --net "host" --mount type=bind,source=${PWD}/tmp/schemaspy-output/,target=/output --mount type=bind,source=${PWD}/tmp/db-drivers/,target=/drivers/ schemaspy/schemaspy:6.1.0 -t pgsql11 -host localhost:5432 -db realworld-db -u realworld-user -p realworld-pass
	open ./tmp/schemaspy-output/index.html

.PHONY: docs.generate-kdoc
docs.generate-kdoc: ## KDocを生成と表示(gitに含めない)
	./gradlew dokkaHtml
	open build/dokka/html/index.html

################################################################################
# OpenAPI Generator
################################################################################
.PHONY: openapi.generate-api-doc
openapi.generate-api-doc: ## スキーマファイル -> ドキュメントを生成
	./gradlew :generateApiDoc
	@echo "Please command. 'open ./build/openapi/doc/index.html'"

.PHONY: openapi.generate-api-server
openapi.generate-api-server: ## スキーマファイル -> サーバー側のコードを生成
	rm -rf ./build/openapi/server-code/
	./gradlew :generateApiServer
	@echo "Please command. 'open ./build/openapi/server-code/'"


################################################################################
# Utility-Command help
################################################################################
.DEFAULT_GOAL := help

################################################################################
# マクロ
################################################################################
# Makefileの中身を抽出してhelpとして1行で出す
# $(1): Makefile名
define help
  grep -E '^[\.a-zA-Z0-9_-]+:.*?## .*$$' $(1) \
  | grep --invert-match "## non-help" \
  | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
endef

################################################################################
# タスク
################################################################################
.PHONY: help
help: ## Make タスク一覧
	@echo '######################################################################'
	@echo '# Makeタスク一覧'
	@echo '# $$ make XXX'
	@echo '# or'
	@echo '# $$ make XXX --dry-run'
	@echo '######################################################################'
	@echo $(MAKEFILE_LIST) \
	| tr ' ' '\n' \
	| xargs -I {included-makefile} $(call help,{included-makefile})
