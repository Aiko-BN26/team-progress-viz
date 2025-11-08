# AGENTS.md
このドキュメントは、Team Progress Visualizationアプリケーションのフロントエンドにおけるエージェントの役割と機能について説明します。

## ページ構成

- `/` : トップ（リダイレクトのみ）
    - `/enroll-organizations` : 組織登録ページ
    - `/organizations` : 組織一覧ページ
        - `/organizations/[id]` : 組織トップページ
- `/login` : ログイン

### タスク完了時の必須チェック
1. npm run lint - ESLint実行
2. npx tsc --noEmit - TypeScript型チェック