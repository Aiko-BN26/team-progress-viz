# Team Progress Viz Backend API

## Authentication Endpoints

---

### GET `/api/auth/github/login`
- GitHub OAuth フローを開始し、現在のセッションにバインドされた Authorization Url を返します。
- **Success 200** response body:
  ```json
  {
    "authorizationUrl": "https://github.com/login/oauth/authorize?..."
  }
  ```
- **Failure 500** response body:
  ```json
  {
    "error": "<failure reason>"
  }
  ```

---

### GET `/api/auth/github/callback`
- GitHub は、ユーザーの承認後、`code` (認可コード) および `state` (CSRF) クエリパラメータを使用してリダイレクトします。
- 状態トークンを検証し、コードをアクセストークンと交換し、認証されたユーザーを HTTP セッションに保存します。
- **Success 302**: `FrontendProperties.successRedirectUrlWithStatus()` が返す URL(e.g. `/auth/callback?status=success`) へ 302 リダイレクト。
- **Failure 302**: `FrontendProperties.errorRedirectUrl("認証に失敗しました")` が返す URL(e.g. `/auth/callback?status=error&message=...`) へ 302 リダイレクト。

---

### GET `/api/auth/session`
- 現在の HTTP セッションから認証されたユーザーを読み取ります。
- **Success 200** response body:
  ```json
  {
    "id": 12345,
    "login": "octocat",
    "name": "The Octocat",
    "avatarUrl": "https://avatars.githubusercontent.com/u/12345"
  }
  ```
- **Unauthorized 401** response body: empty.

---

### POST `/api/auth/logout`
- 現在の HTTP セッションを無効化します。
- **Success 204** response body: empty.

---

## エラーハンドリング
- JSON を返す API エンドポイントは、 `error` メッセージを含む 500 レスポンスに変換します。
```json
{
  "error": "State parameter mismatch"
}
```
- `AuthController` で発生した予期しない例外はログに記録され、一般的な 500 レスポンスペイロードとして返されます。
```json
{
  "error": "Unexpected error occurred"
}
```