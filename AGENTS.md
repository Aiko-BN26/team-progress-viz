# AGENTS.md
このドキュメントは、Team Progress Visualizationアプリケーションのリポジトリにおけるエージェントの役割と機能についての説明です。

## アプリ名(仮)
「チーム開発における進捗可視化アプリ」

## 目的
チーム開発で利用するプロジェクト・タスク管理アプリとして既存のツール「Backlog」がある。Backlogの下位互換で、数人程度のゆるめのチーム開発向けにしたアプリを作成したいと考えた。メンバー同士で適度なプレッシャーを感じられるようにする。

## 概要
このディレクトリは、Team Progress Visualizationプロジェクトのリポジトリが二つ含まれています。
1. frontend: Next.jsを用いたフロントエンドアプリケーション
2. backend: javaを用いたバックエンドシステム


## 技術スタック
- フレームワーク: Next.js
- 言語: TypeScript
- スタイリング: Tailwind CSS
- UIコンポーネント: Shadcn/UI
- バックエンド: Java21 (Spring Boot)
- データベース: Supabase (PostgreSQL)
- ホスト: Next.js/Vercel, Java/Render?
- 定期実行: GitHub Actions

## エージェント行動ガイドライン
- 英語でthinkして、日本語でoutputしてください。
- 各ディレクトリのREADME.mdやAGENTS.mdを参照し、プロジェクト構成や開発パターンを理解してください。
- 余計な機能は追加せず、シンプルな実装を心がけてください。