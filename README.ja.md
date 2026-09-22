# Loggi Server

[![Backend CI](https://github.com/PiscesTrio/Loggi_server/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/PiscesTrio/Loggi_server/actions/workflows/backend-ci.yml)
[![Java](https://img.shields.io/badge/Java-21-informational)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-informational)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-informational)](LICENSE)

[English](README.md) · [日本語](README.ja.md)

> デプロイに耐える作りにはしていない。実データには接続しないこと。

物流管理システムの REST API。倉庫と在庫、商品、配送依頼とその追跡、車両とドライバー、販売、
管理者、監査ログを扱う。

> フロントエンドのリポジトリ: https://github.com/PiscesTrio/Loggi_app

## 技術スタック

| | |
| --- | --- |
| 言語 / ランタイム | Java 21 |
| フレームワーク | Spring Boot 4.1.0 |
| セキュリティ | Spring Security 7、ステートレス JWT |
| 永続化 | Spring Data JPA / Hibernate、`ddl-auto: validate` |
| スキーマ | Flyway マイグレーション |
| データベース | MySQL 8 |
| キャッシュ | Redis — ワンタイムコードとレート制限のカウンタ |
| ドキュメント | springdoc OpenAPI 3、Swagger UI |
| 運用 | Spring Boot Actuator |
| ビルド | Maven（ラッパが 3.9.6 を固定） |
| テスト | JUnit 5、Mockito、Testcontainers 2.0、GreenMail、JaCoCo |

## 主な機能

- **認証** — JWT。パスワードによるログイン、またはメールアドレスに送るワンタイムコードで
  発行する。パスワードは delegating encoder のハッシュで保存し、平文では持たない。
- **認可** — 六つのロール（`ROLE_SUPER_ADMIN`、`ROLE_ADMIN`、`ROLE_COMMODITY`、
  `ROLE_EMPLOYEE`、`ROLE_SALE`、`ROLE_WAREHOUSE`）を、URL ではデフォルト拒否のチェーンで、
  メソッドでは `@PreAuthorize` が担う。
- **倉庫と在庫** — 倉庫ごとの在庫数と、それを動かす入出庫。一つのトランザクションで書くので、
  中途半端な移動が記録されることはない。
- **商品と販売** — カタログと販売記録。
- **配送依頼** — ドライバー・車両・出発倉庫を指す依頼と、その追跡履歴。承認するとドライバーと
  車両を押さえ、完了すると解放する。
- **監査ログ** — 監査対象の呼び出しとログイン試行のすべてをアスペクトが記録し、返すときは
  ページングする。

## セットアップ

### 前提条件

- JDK 21
- Docker（MySQL と Redis、および結合テスト用）
- Maven は不要 — ラッパ（`./mvnw`）を使う

### 設定

`.env.example` が、このサービスが読む変数をすべて挙げた一覧である。`.env` にコピーして
実際の値を入れる。`.env` は gitignore されており、コミットしてはならない。

`src/main/resources/application.yaml` の既定値は、動く値ではなく意図的に動かない
プレースホルダである。動いてしまう既定値は、そのまま出荷される既定値だからだ:

| 対象 | 変数 | 既定値 |
| --- | --- | --- |
| データベース | `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | `localhost` / `3306` / `loggi` / `root` / プレースホルダ |
| Redis | `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| JWT | `JWT_SECRET` | `CHANGE_ME` — **この値では起動を拒否する**。32 バイト未満でも同じ |
| メール | `MAIL_HOST` / `MAIL_PORT` / `MAIL_SSL_ENABLE` / `MAIL_STARTTLS_ENABLE` / `MAIL_USERNAME` / `MAIL_PASSWORD` | いずれもプレースホルダ。ホストの既定値は解決できない `.invalid` 名なので、設定を忘れたデプロイははっきり失敗する。ポートと転送方式はホストと一緒に決まる — 暗黙 TLS なら 465、STARTTLS なら 587。パスワードはアカウントのものではなく、プロバイダのアプリパスワードである |
| プロファイル | `SPRING_PROFILES_ACTIVE` | `dev` — これはこのサービスの変数ではなく Spring 自身の変数なので `.env.example` には無い。環境側で指定する |
| 認証コード | `VERIFICATION_CODE_TTL_SECONDS` / `VERIFICATION_SEND_COOLDOWN_SECONDS` / `VERIFICATION_MAX_ATTEMPTS` / `VERIFICATION_LOCK_SECONDS` | `300` / `60` / `5` / `900` |
| ノイズ | `JPA_SHOW_SQL` / `MAIL_DEBUG` | `dev` 以外では off |

`docker compose` は `.env` を自動で読む。素の `./mvnw spring-boot:run` では自分で環境変数を
export すること。

### プロファイル

`dev`（既定）と `prod` の違いは開発上の利便性だけで、どの設定も実体は環境変数である —
プロファイルが既定値を置き、環境がそれを上書きする。環境によって変わってはならないもの
（Flyway、スキーマ検証、プレースホルダを拒否する JWT シークレット）は `application.yaml` に
あるので、プロファイル設定のどの組み合わせからも危険な本番は生まれない。

### 実行

```bash
cp .env.example .env          # その上で実際の値を入れる
docker compose up --build     # アプリ + MySQL 8
```

すでにデータベースがあるなら:

```bash
./mvnw spring-boot:run
```

API は **8088** で待ち受ける。

### ビルド

```bash
./mvnw clean package
```

## API

すべて `/api` の下にあり、どのレスポンスも同じエンベロープに包まれる:

```json
{ "code": 200, "status": true, "msg": null, "data": { } }
```

`code` が常に 200 ではなく HTTP ステータスを繰り返すのは、ステータス行と食い違う本文が、
失敗を成功に見せてしまうからである。例外は `204 No Content` ひとつで、これは本文を持たない。

[`docs/contract-changes.md`](docs/contract-changes.md) に、ドメインモデルを作り直す前後で
この通信フォーマットがどう変わったかを記録してある。エンティティから読み取ったものではなく、
動いているサーバーに対して測った値である。

### 認証

トークンは標準の Bearer 形式で送る:

```
Authorization: Bearer <token>
```

発行元は `POST /api/admin/login/password` と `POST /api/admin/login/email` である。

公開しているエンドポイントと、公開せざるを得ない理由:

| エンドポイント | 理由 |
| --- | --- |
| `POST /api/admin/login/password`、`/login/email` | トークンの出どころ |
| `POST /api/admin/verification-code` | メールログインの第一段階 |
| `GET /api/admin/hasInit` | アカウントの無い新規インストールで尋ねられる |
| `POST /api/admin/init` | 最初のアカウントを作る。認証ではなく `hasInit` で守る — まだ認証される対象が存在しないため |
| `GET /actuator/health` | オーケストレータは、アプリがトークンを発行できるようになる前にここを叩く |
| `/v3/api-docs`、`/swagger-ui.html` | 下記参照 |

それ以外はすべてデフォルトで拒否する。

### ドキュメント

- Swagger UI — http://localhost:8088/swagger-ui.html
- OpenAPI ドキュメント — http://localhost:8088/v3/api-docs

コントローラから生成しているので、手書きのファイルのようにコードから乖離することがない。

公開しているのは意図的である。記述されているエンドポイントはどれもそれ自体が認証されるので、
記述を隠すのは管理策ではなく単なる隠蔽であり、API を開いて読めること自体がここでは要点である。
実際のデプロイなら逆の判断をするだろう — 公開した分だけ、その中で最も弱い所への道は短く
なるからである。

### ヘルス

- `GET /actuator/health` — `UP` か `DOWN` か、それだけ。どの構成要素が落ちているかを示す詳細
  表示は、このエンドポイントが匿名の呼び出しに答える以上は出さない。
- `/actuator` 配下のそれ以外はトークンを要求する。

メールのヘルスインジケータは無効にしてある。これは SMTP に対して認証を試みるが、ここの
認証情報は設計上プレースホルダであり、落ちているインジケータは全体のステータスを `DOWN` に
引きずる — つまり readiness プローブが、アプリはすべてのリクエストに正しく応えているのに、
コンテナを永久にサービスから外し続けることになる。データベースと Redis のインジケータは
入れたままである。到達できない第三者の SMTP はこのアプリの異常ではないが、到達できない
Redis は異常だからである。

### 規約

| | |
| --- | --- |
| 作成 | `POST /resource` → **201**、作られたリソースを返す |
| 取得 | `GET /resource`、`GET /resource/{id}` |
| 更新 | `PUT /resource/{id}` — id はパスにあり、本文には決して入れない |
| 削除 | `DELETE /resource/{id}` → **204**、本文なし |
| 不正なリクエスト | **400**、失敗したフィールド自身のメッセージを `msg` に入れる |
| 参照先が無い | **404**、その id を示す |
| 業務上の拒否 | **409** — 例えば、すでに配送中のドライバーを割り当てようとしたとき |

リクエストとレスポンスは DTO とビュー型であり、エンティティではない。リクエスト型はどれも
`id` を持たない。作成時の id は、Hibernate には「更新すべき既存の行」と読まれるからである。

ページングするリストは二つ — `GET /api/systemlog` と `GET /api/loginlog` — 監査対象のリクエスト
ごと、ログイン試行ごとに一行ずつ、際限なく増えるためである:

```json
{ "items": [ ], "page": 0, "size": 20, "totalItems": 137, "totalPages": 7 }
```

`page` と `size` を取り、既定は 20 件、新しい順である。残りのリストは上限があるのでそのまま
返す。包んだところで、呼び出し側が既に手にしているものを取り出す手間が増えるだけである。

### 初回セットアップ

`GET /api/admin/hasInit` がスーパー管理者の有無を答える。居なければ `POST /api/admin/init` が
最初の一人を作る — 一度だけ。二度目は拒否されるので、このエンドポイントを使って稼働中の
システムに管理者を作り出すことはできない。

デモ用のシードには既に一人入っている: `demo@loggi.example` / `demo1234`。

## データベーススキーマ

所有者は Hibernate ではなく Flyway である。マイグレーションは
`src/main/resources/db/migration` にあり、データソースに他の何かが触れる前に走る:

| スクリプト | 内容 |
| --- | --- |
| `V1__baseline.sql` | Flyway が引き継いだ時点の、エンティティが定義するとおりのスキーマ |
| `V2__unique_constraints_and_indexes.sql` | `ddl-auto` には決して作れなかった一意制約とインデックス |
| `V3__timestamps_as_datetime.sql` | `varchar` のタイムスタンプが本物の `datetime` 列になる |
| `V4__money_and_quantity_types.sql` | 金額が `DECIMAL` に、数量が文字列でなくなる |
| `V5__distribution_associations.sql` | 依頼がドライバー・車両・倉庫を指すようになる |
| `V6__inventory_and_track_associations.sql` | 残っていた裸の外部キーが本物の外部キーになる |
| `V7__roles_collection_and_log_enums.sql` | ロールが行になり、監査ログが表示ラベルの保存をやめる |
| `V8__backfill_denormalised_commodity_names.sql` | 呼び出し側が入れ忘れた非正規化済みの商品名を埋める |
| `V9__wire_values_become_identifiers.sql` | 性別・車両種別・取扱注意が中国語の表示テキストをやめ、識別子になる |
| `V10__audit_columns_stop_storing_labels.sql` | 監査ログのモジュール列とブラウザ列が列挙名になる |

ここから二つの規則が出てくる:

- **適用済みのスクリプトは決して編集しない。** Flyway はスクリプトごとにチェックサムを持って
  いるので、一つ変えれば既存のデータベースはすべて検証に失敗する。スキーマ変更は `V11`、
  `V12`、… と追加していく。
- **`ddl-auto` はどのプロファイルでも `validate`。** Hibernate はもうデータベースを変更せず、
  エンティティとマイグレーションが一致しているかを確認するだけで、一致しなければ起動を拒否
  する。マイグレーションの漏れは本番で静かにずれるのではなく、CI で落ちる。

`flyway_schema_history` テーブルを持たない既存のデータベースは、拒否せず引き取る。
`baseline-on-migrate` がバージョン 1 として記録し、`V2` 以降を適用する。ただし `V2` の一意制約
は、既に重複を含むデータの上では失敗する — 先にデータを整理すること。

デモデータは `src/main/resources/data.sql` が投入する。`seed-` という id 接頭辞で削除と再挿入を
行うので、繰り返し実行でき、アプリ経由で作られた行には触れない。

## アーキテクチャ

```
com.example.api/
├── annotation/      # @Log, @DisableBaseResponse
├── aspect/          # 監査ログのアスペクト
├── config/          # JPA 監査、OpenAPI ドキュメントのメタデータ
├── controller/      # /api/* の REST エンドポイント
├── exception/       # BizException — 自分のステータスを持つ失敗
├── handler/         # レスポンスエンベロープと、例外からステータスへの対応
├── model/
│   ├── dto/         # リクエスト型。Bean Validation の制約付き
│   ├── entity/      # JPA エンティティ — クライアントには直列化しない
│   ├── enums/       # ドメインの列挙。名前で永続化する
│   ├── support/     # レスポンスエンベロープ
│   └── vo/          # ビュー型 — エンドポイントが実際に返すもの
├── repository/      # Spring Data JPA のリポジトリ
├── security/        # JWT フィルタとセキュリティチェーン
├── service/         # ビジネスロジック（インタフェースと実装）
└── utils/           # JWT、IP、ブラウザのヘルパ
```

## テスト

```bash
./mvnw test                 # 単体テストのみ — Docker 不要
./mvnw verify -DskipITs     # + JaCoCo レポートとカバレッジゲート。まだ Docker 不要
./mvnw verify               # + *IT 結合テスト（Testcontainers → Docker が要る）
```

クラス単位で走らせるなら `./mvnw test -Dtest=ClassName`。

結合テスト（`*IT`）は実際の `mysql:8.0` と `redis:7-alpine` のコンテナを起動し、認証コードの
テストは GreenMail の SMTP サーバーを起動する。Docker Engine 29 以降では `-Dapi.version=1.44` を
付けること。Testcontainers が同梱するクライアントは、デーモンが拒否するバージョンを交渉して
しまう。

これらは単体テストを遅くしただけのものではない。コンテナは空の状態で起動するので、どの
テストも `V1` から Flyway を走らせ、その上で Hibernate がマイグレーションの作った形に対して
エンティティを検証する — つまり一つ一つが、スキーマとモデルがまだ一致しているかの確認でも
ある。

JaCoCo の HTML レポートは `target/site/jacoco/index.html`。`pom.xml` のカバレッジゲートは目標
値ではなく、実際の実行から測った下限であり、下がることはない。
