# AGENTS.md
このドキュメントは、Team Progress Visualizationアプリケーションのフロントエンドにおけるエージェントの役割と機能について説明します。

## ページ構成

- `/` : トップ（リダイレクトのみ）
    - `/enroll-organizations` : 組織登録ページ
    - `/organizations` : 組織一覧ページ
        - `/organizations/[id]` : 組織トップページ
- `/login` : ログイン

## 重要ファイル
- `proxy.ts` : middlewareファイル規約は廃止され、proxyに名前変更されました。詳細:https://nextjsjp.org/docs/app/api-reference/file-conventions/proxy#proxy%E3%81%B8%E3%81%AE%E7%A7%BB%E8%A1%8C

### タスク完了時の必須チェック
1. npm run lint - ESLint実行
2. npx tsc --noEmit - TypeScript型チェック