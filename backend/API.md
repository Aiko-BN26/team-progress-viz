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

---

## GitHub Organization Endpoints

### GET `/api/github/organizations`
- セッションに保存された GitHub アクセストークンを使用して、参加している全ての Organization を返します。
- **Success 200** response body:
  ```json
  [
    {
      "id": 123,
      "login": "octo-org",
      "name": "Octo Org",
      "description": "our organization",
      "avatarUrl": "https://avatars.githubusercontent.com/u/123",
      "htmlUrl": "https://github.com/octo-org"
    }
  ]
  ```
- **Unauthorized 401** response body: empty.

---

### GET `/api/github/organizations/{organization}/repositories`
- 指定した Organization の全リポジトリを取得します。
- **Success 200** response body:
  ```json
  [
    {
      "id": 456,
      "name": "octo-repo",
      "description": "Repository description",
      "htmlUrl": "https://github.com/octo-org/octo-repo",
      "language": "TypeScript",
      "stargazersCount": 42,
      "forksCount": 7,
      "defaultBranch": "main",
      "isPrivate": true,
      "archived": false
    }
  ]
  ```
- **Unauthorized 401** response body: empty。
- **Failure 500** response body (GitHub API エラーなど):
  ```json
  {
    "error": "Failed to fetch GitHub repositories: 404 Not Found"
  }
  ```