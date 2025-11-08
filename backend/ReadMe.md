# APIエンドポイントリファレンス

## 認証 (`/api/auth`)

### GET /api/auth/github/login
- **説明**: GitHub OAuth を開始し、認可 URL を生成。
- **レスポンス (200)**
  ```json
  {
    "authorizationUrl": "https://github.com/login/oauth/authorize?..."
  }
  ```

### GET /api/auth/github/callback
- **説明**: GitHub からのリダイレクトを受け取り認証を確定。
- **クエリ**: `code` (必須), `state` (任意、セッションに保存した値と照合)
- **レスポンス**
  - 認証成功: `302 Found` → `app.frontend.success-path?status=success`
  - 認証失敗: `302 Found` → `app.frontend.error-path?status=error&message=...`

### GET /api/auth/session
- **説明**: 現在のセッションに紐づく認証済みユーザー (`AuthenticatedUser`) を返却。
- **レスポンス**
  - 200 OK
    ```json
    {
      "id": 123,
      "login": "octocat",
      "name": "The Octocat",
      "avatarUrl": "https://..."
    }
    ```
  - 401 Unauthorized: セッションにユーザーなし。

### POST /api/auth/logout
- **説明**: セッションを破棄。
- **レスポンス**: `204 No Content`

---

## ユーザー (`/api/users`)

### GET /api/users/me
- **説明**: 自ユーザー情報 (`UserResponse`) を返却し、必要に応じてオンボーディングジョブをキューイング。
- **レスポンス (200)**
  ```json
  {
    "id": 1,
    "githubId": 999999,
    "login": "octocat",
    "name": "The Octocat",
    "avatarUrl": "https://..."
  }
  ```
- セッション未認証時は 401。

### GET /api/users/me/onboarding-jobs
- **説明**: セッションに保存されたオンボーディングジョブ (`UserOnboardingJobResponse`) を取得。
- **レスポンス (200)**
  ```json
  [
    {
      "organizationId": 10,
      "organizationLogin": "example-org",
      "jobId": "job-sync-org-..."
    }
  ]
  ```
- ジョブが無ければ空配列。

### DELETE /api/users/me
- **説明**: 自ユーザー削除ジョブを登録しセッションを失効。
- **レスポンス (202)** (`JobSubmissionResponse`)
  ```json
  {
    "jobId": "job-delete-user-...",
    "status": "queued"
  }
  ```

---

## GitHub プロキシ (`/api/github`)

### GET /api/github/organizations
- **説明**: GitHub アクセストークンで到達可能な組織 (`GitHubOrganization`) 一覧。
- **レスポンス (200)**
  ```json
  [
    {
      "id": 1,
      "login": "example-org",
      "name": "Example Org",
      "description": "...",
      "avatarUrl": "https://...",
      "htmlUrl": "https://github.com/example-org"
    }
  ]
  ```
- トークン未保持時は 401。

### GET /api/github/organizations/{organization}/repositories
- **説明**: 指定組織のリポジトリ (`GitHubRepository`) 一覧。
- **レスポンス (200)**
  ```json
  [
    {
      "id": 100,
      "name": "team-progress-viz",
      "description": "...",
      "htmlUrl": "https://github.com/example/team-progress-viz",
      "language": "TypeScript",
      "stargazersCount": 10,
      "forksCount": 2,
      "defaultBranch": "main",
      "isPrivate": false,
      "archived": false
    }
  ]
  ```

### GET /api/github/organizations/{organization}/members
- **説明**: 指定組織のメンバー (`GitHubOrganizationMember`) 一覧。
- **レスポンス (200)**
  ```json
  [
    {
      "id": 200,
      "login": "octocat",
      "avatarUrl": "https://...",
      "htmlUrl": "https://github.com/octocat",
      "type": "User",
      "siteAdmin": false
    }
  ]
  ```

---

## 組織管理 (`/api/organizations`)

### GET /api/organizations
- **説明**: 登録済み組織 (`OrganizationSummaryResponse`) 一覧。
- **レスポンス (200)**
  ```json
  [
    {
      "id": 1,
      "githubId": 99999,
      "login": "example-org",
      "name": "Example Org",
      "avatarUrl": "https://...",
      "description": "..."
    }
  ]
  ```

### POST /api/organizations
- **説明**: 組織登録および初回同期ジョブのキューイング。
- **リクエストボディ**
  ```json
  {
    "login": "example-org",
    "defaultLinkUrl": "https://example.com/board"
  }
  ```
- **レスポンス (202)** (`OrganizationRegistrationResponse`)
  ```json
  {
    "organizationId": 1,
    "githubId": 99999,
    "login": "example-org",
    "name": "Example Org",
    "htmlUrl": "https://github.com/example-org",
    "status": "queued",
    "jobId": "job-sync-org-...",
    "syncedRepositories": 0
  }
  ```

### POST /api/organizations/ensure-sync
- **説明**: セッションに紐づくユーザーがアクセス可能な組織の同期ジョブをまとめて起動。
- **レスポンス (202)** (`OrganizationEnsureSyncResponse`)
  ```json
  {
    "jobs": [
      {
        "organizationId": 1,
        "organizationLogin": "example-org",
        "jobId": "job-sync-org-..."
      }
    ]
  }
  ```

### POST /api/organizations/{organizationId}/sync
- **説明**: 指定組織の再同期ジョブを登録。
- **レスポンス (202)** (`JobSubmissionResponse`)
  ```json
  {
    "jobId": "job-sync-org-...",
    "status": "queued"
  }
  ```

### GET /api/organizations/{organizationId}/sync/status
- **説明**: リポジトリ同期ステータス (`RepositorySyncStatusResponse`) 一覧。
- **レスポンス (200)**
  ```json
  [
    {
      "repositoryId": 50,
      "repositoryFullName": "example-org/repo",
      "lastSyncedAt": "2024-09-13T12:34:56Z",
      "lastSyncedCommitSha": "abcdef...",
      "errorMessage": null
    }
  ]
  ```

### GET /api/organizations/{organizationId}
- **説明**: 組織ダッシュボード詳細 (`OrganizationDetailResponse`) を返却。
- **レスポンス (200)**
  ```json
  {
    "organization": {
      "id": 1,
      "githubId": 99999,
      "login": "example-org",
      "name": "Example Org",
      "description": "...",
      "avatarUrl": "https://...",
      "htmlUrl": "https://github.com/example-org",
      "defaultLinkUrl": "https://example.com/board"
    },
    "members": [
      {
        "userId": 10,
        "githubId": 12345,
        "login": "octocat",
        "name": "The Octocat",
        "avatarUrl": "https://...",
        "role": "member"
      }
    ],
    "repositories": [
      {
        "id": 50,
        "githubId": 54321,
        "fullName": "example-org/repo",
        "htmlUrl": "https://github.com/example-org/repo",
        "language": "Java",
        "stargazersCount": 5,
        "forksCount": 1,
        "updatedAt": "2024-09-13T12:34:56Z"
      }
    ],
    "activitySummaryLast7Days": {
      "commitCount": 42,
      "additions": 1234,
      "deletions": 432,
      "activeMembers": 3
    },
    "pullRequestSummary": {
      "openCount": 2,
      "closedCount": 10,
      "mergedCount": 8
    },
    "recentPullRequests": [
      {
        "id": 70,
        "number": 15,
        "repositoryId": 50,
        "repositoryFullName": "example-org/repo",
        "title": "Add new feature",
        "state": "closed",
        "merged": true,
        "htmlUrl": "https://github.com/...",
        "author": {
          "userId": 10,
          "githubId": 12345,
          "login": "octocat",
          "name": "The Octocat",
          "avatarUrl": "https://..."
        },
        "mergedBy": {
          "userId": 11,
          "githubId": 67890,
          "login": "hubot",
          "name": "Hubot",
          "avatarUrl": "https://..."
        },
        "additions": 100,
        "deletions": 20,
        "changedFiles": 5,
        "createdAt": "2024-09-13T10:00:00Z",
        "updatedAt": "2024-09-13T12:00:00Z",
        "mergedAt": "2024-09-13T11:00:00Z",
        "closedAt": "2024-09-13T12:00:00Z"
      }
    ],
    "recentCommits": [
      {
        "id": 80,
        "sha": "abcdef...",
        "message": "feat: ...",
        "repositoryId": 50,
        "repositoryFullName": "example-org/repo",
        "htmlUrl": "https://github.com/.../commit/abcdef",
        "authorName": "The Octocat",
        "committerName": "Hubot",
        "committedAt": "2024-09-13T09:00:00Z",
        "pushedAt": "2024-09-13T09:05:00Z"
      }
    ],
    "recentComments": [
      {
        "id": 90,
        "user": {
          "userId": 10,
          "githubId": 12345,
          "login": "octocat",
          "name": "The Octocat",
          "avatarUrl": "https://..."
        },
        "targetType": "pull_request",
        "targetId": 70,
        "parentCommentId": null,
        "content": "進捗どうですか?",
        "createdAt": "2024-09-13T08:00:00Z",
        "updatedAt": "2024-09-13T08:10:00Z"
      }
    ]
  }
  ```

### DELETE /api/organizations/{organizationId}
- **説明**: 組織削除ジョブを登録。
- **レスポンス (202)**: `JobSubmissionResponse`

---

## 組織アクティビティ (`/api/organizations/{organizationId}` 配下)

### GET /api/organizations/{organizationId}/activity/summary
- **説明**: 指定期間の活動サマリー (`ActivitySummaryItemResponse`)。
- **クエリ**: `startDate`(必須), `endDate`(必須), `groupBy`(任意)
- **レスポンス (200)**
  ```json
  [
    {
      "userId": 10,
      "login": "octocat",
      "name": "The Octocat",
      "avatarUrl": "https://...",
      "commitCount": 10,
      "filesChanged": 25,
      "additions": 500,
      "deletions": 200,
      "availableMinutes": 240
    }
  ]
  ```

### GET /api/organizations/{organizationId}/git-commit/feed
- **説明**: コミットフィード (`CommitFeedResponse`) のページング取得。
- **クエリ**: `cursor`(任意, 数値文字列), `limit`(任意)
- **レスポンス (200)**
  ```json
  {
    "items": [
      {
        "id": 80,
        "sha": "abcdef...",
        "repositoryFullName": "example-org/repo",
        "message": "feat: ...",
        "authorName": "The Octocat",
        "committerName": "Hubot",
        "committedAt": "2024-09-13T09:00:00Z",
        "url": "https://github.com/..."
      }
    ],
    "nextCursor": "81"
  }
  ```

### GET /api/organizations/{organizationId}/dashboard
- **説明**: `DashboardResponse` (statuses/commits/comments を統合)。
- **レスポンス (200)**
  ```json
  {
    "statuses": [
      {
        "userId": 10,
        "login": "octocat",
        "name": "The Octocat",
        "avatarUrl": "https://...",
        "availableMinutes": 240,
        "status": "working",
        "statusMessage": "レビュー対応",
        "updatedAt": "2024-09-13T08:30:00Z"
      }
    ],
    "commits": [
      {
        "id": 80,
        "sha": "abcdef...",
        "repositoryFullName": "example-org/repo",
        "message": "feat: ...",
        "authorName": "The Octocat",
        "committedAt": "2024-09-13T09:00:00Z",
        "url": "https://github.com/..."
      }
    ],
    "comments": [
      {
        "commentId": 90,
        "userId": 10,
        "login": "octocat",
        "avatarUrl": "https://...",
        "content": "進捗どうですか?",
        "createdAt": "2024-09-13T08:00:00Z"
      }
    ]
  }
  ```

### POST /api/organizations/{organizationId}/comments
- **説明**: コメントを作成 (`CommentCreateRequest`)。成功時 `201 Created` と `Location: /api/organizations/{organizationId}/comments/{commentId}`。
- **レスポンス (201)** (`CommentCreateResponse`)
  ```json
  {
    "commentId": 90,
    "createdAt": "2024-09-13T08:00:00Z"
  }
  ```

### GET /api/organizations/{organizationId}/comments
- **説明**: コメント一覧 (`CommentListItemResponse`)。`targetType` / `targetId` でフィルタ可能。
- **レスポンス (200)**
  ```json
  [
    {
      "commentId": 90,
      "userId": 10,
      "login": "octocat",
      "name": "The Octocat",
      "avatarUrl": "https://...",
      "targetType": "pull_request",
      "targetId": 70,
      "parentCommentId": null,
      "content": "進捗どうですか?",
      "createdAt": "2024-09-13T08:00:00Z",
      "updatedAt": "2024-09-13T08:10:00Z"
    }
  ]
  ```

### DELETE /api/organizations/{organizationId}/comments/{commentId}
- **説明**: コメント削除。
- **レスポンス**: `204 No Content`

### POST /api/organizations/{organizationId}/statuses
- **説明**: ステータス登録/更新 (`StatusUpdateRequest`)。
- **レスポンス (200)** (`StatusUpdateResponse`)
  ```json
  {
    "personalStatus": {
      "submitted": true,
      "status": "working",
      "statusMessage": "レビュー対応",
      "lastSubmittedAt": "2024-09-13T08:00:00Z",
      "commitCount": 3,
      "capacityHours": 6,
      "streakDays": 4,
      "latestPrUrl": "https://github.com/..."
    },
    "member": {
      "memberId": "u-123",
      "displayName": "The Octocat",
      "avatarUrl": "https://...",
      "status": "working",
      "statusMessage": "レビュー対応",
      "lastSubmittedAt": "2024-09-13T08:00:00Z",
      "commitCount": 3,
      "capacityHours": 6,
      "streakDays": 4,
      "latestPrUrl": "https://github.com/..."
    },
    "summary": {
      "activeToday": 5,
      "pendingStatusCount": 2
    }
  }
  ```

### GET /api/organizations/{organizationId}/statuses
- **説明**: 日次ステータス一覧 (`StatusListItemResponse`)。`date` は `YYYY-MM-DD` 形式。
- **レスポンス (200)**
  ```json
  [
    {
      "statusId": 123,
      "userId": 10,
      "login": "octocat",
      "name": "The Octocat",
      "avatarUrl": "https://...",
      "date": "2024-09-13",
      "availableMinutes": 240,
      "status": "working",
      "statusMessage": "レビュー対応",
      "updatedAt": "2024-09-13T08:30:00Z"
    }
  ]
  ```

### DELETE /api/organizations/{organizationId}/statuses/{statusId}
- **説明**: ステータス削除。
- **レスポンス**: `204 No Content`

### GET /api/organizations/{organizationId}/pulls/feed
- **説明**: PR フィード (`PullRequestFeedResponse`)。
- **クエリ**: `cursor`(任意, 数値文字列), `limit`(任意)
- **レスポンス (200)**
  ```json
  {
    "items": [
      {
        "id": 70,
        "number": 15,
        "title": "Add new feature",
        "repositoryFullName": "example-org/repo",
        "state": "closed",
        "user": {
          "userId": 10,
          "githubId": 12345,
          "login": "octocat",
          "avatarUrl": "https://..."
        },
        "createdAt": "2024-09-13T10:00:00Z",
        "updatedAt": "2024-09-13T12:00:00Z",
        "url": "https://github.com/..."
      }
    ],
    "nextCursor": "71"
  }
  ```

---

## リポジトリアクティビティ (`/api/repositories`)

### POST /api/repositories/{repositoryId}/pulls/sync
- **説明**: 指定リポジトリの PR 同期ジョブを登録。
- **レスポンス (202)**
  ```json
  {
    "jobId": "job-sync-prs-...",
    "status": "queued"
  }
  ```

### GET /api/repositories/{repositoryId}/pulls
- **説明**: PR 一覧 (`PullRequestListItemResponse`)。
- **クエリ**: `state`, `limit`, `page`
- **レスポンス (200)**
  ```json
  [
    {
      "id": 70,
      "number": 15,
      "title": "Add new feature",
      "state": "closed",
      "user": {
        "userId": 10,
        "githubId": 12345,
        "login": "octocat",
        "avatarUrl": "https://..."
      },
      "createdAt": "2024-09-13T10:00:00Z",
      "updatedAt": "2024-09-13T12:00:00Z",
      "repositoryFullName": "example-org/repo"
    }
  ]
  ```

### GET /api/repositories/{repositoryId}/pulls/{pullNumber}
- **説明**: PR 詳細 (`PullRequestDetailResponse`)。
- **レスポンス (200)**
  ```json
  {
    "id": 70,
    "number": 15,
    "title": "Add new feature",
    "body": "...",
    "state": "closed",
    "merged": true,
    "htmlUrl": "https://github.com/...",
    "user": {
      "userId": 10,
      "githubId": 12345,
      "login": "octocat",
      "avatarUrl": "https://..."
    },
    "mergedBy": {
      "userId": 11,
      "githubId": 67890,
      "login": "hubot",
      "avatarUrl": "https://..."
    },
    "additions": 100,
    "deletions": 20,
    "changedFiles": 5,
    "createdAt": "2024-09-13T10:00:00Z",
    "updatedAt": "2024-09-13T12:00:00Z",
    "mergedAt": "2024-09-13T11:00:00Z",
    "closedAt": "2024-09-13T12:00:00Z",
    "repositoryFullName": "example-org/repo"
  }
  ```

### GET /api/repositories/{repositoryId}/pulls/{pullNumber}/files
- **説明**: PR の変更ファイル (`PullRequestFileResponse`) 一覧。
- **レスポンス (200)**
  ```json
  [
    {
      "id": 1,
      "path": "src/App.tsx",
      "extension": "tsx",
      "additions": 10,
      "deletions": 2,
      "changes": 12,
      "rawBlobUrl": "https://github.com/..."
    }
  ]
  ```

### GET /api/repositories/{repositoryId}/commits
- **説明**: コミット一覧 (`CommitListItemResponse`)。
- **レスポンス (200)**
  ```json
  [
    {
      "id": 80,
      "sha": "abcdef...",
      "message": "feat: ...",
      "repositoryFullName": "example-org/repo",
      "authorName": "The Octocat",
      "committerName": "Hubot",
      "committedAt": "2024-09-13T09:00:00Z",
      "url": "https://github.com/..."
    }
  ]
  ```

### GET /api/repositories/{repositoryId}/commits/{sha}
- **説明**: コミット詳細 (`CommitDetailResponse`)。
- **レスポンス (200)**
  ```json
  {
    "id": 80,
    "sha": "abcdef...",
    "repositoryId": 50,
    "repositoryFullName": "example-org/repo",
    "message": "feat: ...",
    "url": "https://github.com/.../commit/abcdef",
    "authorName": "The Octocat",
    "authorEmail": "octocat@example.com",
    "committerName": "Hubot",
    "committerEmail": "hubot@example.com",
    "committedAt": "2024-09-13T09:00:00Z",
    "pushedAt": "2024-09-13T09:05:00Z"
  }
  ```

### GET /api/repositories/{repositoryId}/commits/{sha}/files
- **説明**: コミットの変更ファイル (`CommitFileResponse`) 一覧。
- **レスポンス (200)**
  ```json
  [
    {
      "id": 10,
      "path": "src/App.tsx",
      "filename": "App.tsx",
      "extension": "tsx",
      "status": "modified",
      "additions": 10,
      "deletions": 2,
      "changes": 12,
      "rawBlobUrl": "https://github.com/..."
    }
  ]
  ```

---

## ジョブ (`/api/jobs`)

### GET /api/jobs/{jobId}
- **説明**: 任意ジョブのステータス (`JobStatusResponse`) を取得。
- **レスポンス (200)**
  ```json
  {
    "jobId": "job-sync-org-...",
    "type": "job-sync-org",
    "status": "RUNNING",
    "createdAt": "2024-09-13T08:00:00Z",
    "startedAt": "2024-09-13T08:00:10Z",
    "finishedAt": null,
    "errorMessage": null
  }
  ```
- `status` は `QUEUED` / `RUNNING` / `SUCCEEDED` / `FAILED` のいずれか。未認証時は 401、存在しない場合は 404。

---

## Webhook (`/api/webhooks`)

### POST /api/webhooks/github
- **説明**: GitHub Webhook イベントを受信 (署名検証は別途実装想定)。
- **ヘッダー**: `X-GitHub-Event`, `X-GitHub-Delivery`, `X-Hub-Signature-256` (任意)
- **リクエストボディ**: GitHub のペイロード文字列
- **レスポンス (202)**
  ```json
  {
    "status": "queued"
  }
  ```