.PHONY: up
up: ## サーバー起動
	./gradlew bootRun

.PHONY: up.db
up.db: ## db起動
	docker-compose up

.PHONY: down.db
down.db: ## dbを落とす
	docker-compose down

.PHONY: test
test: ## テスト実行
	./gradlew test

.PHONY: test.with-local-db
test.with-local-db: ## テスト(with local db)実行
	./gradlew test withLocalDb

.PHONY: test.e2e
test.e2e: ## e2eテスト実行
	bash e2e/run-api-tests.sh

.PHONY: fmt
fmt: ## format
	./gradlew detekt --auto-correct

.PHONY: lint
lint: ## lint
	./gradlew detekt

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
