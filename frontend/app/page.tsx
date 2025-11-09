"use client";

import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-screen bg-slate-950 text-slate-50">
      <div className="mx-auto flex max-w-6xl flex-col gap-12 px-6 py-20">
        <section className="space-y-6">
          <p className="text-sm uppercase tracking-[0.4em] text-slate-400">チーム進捗を照らす</p>
          <h1 className="text-4xl font-bold leading-tight text-white sm:text-5xl">
            がんばログ
          </h1>
          <p className="text-lg text-slate-300 sm:text-xl">
            がんばログは数人で動くチームのための、ゆるやかなプレッシャーを生む進捗可視化ツールです。日々のタスクを簡単に登録し、誰が何を頑張っているのかがひと目でわかるUIを提供します。
          </p>
          <div className="flex flex-wrap gap-3">
            <Link
              href="/login"
              className="rounded-full bg-amber-500 px-6 py-3 text-base font-semibold text-slate-950 transition hover:bg-amber-400"
            >
              ログインしてはじめる
            </Link>
          </div>
        </section>

        <section className="grid gap-6 md:grid-cols-3">
          {[
            {
              title: "シンプルな登録",
              description: "プロジェクト・タスクをすばやく書き出し、チームと共有できます。",
            },
            {
              title: "進捗の見える化",
              description: "誰がどこでつまずいているかを色付きのカードで把握。",
            },
            {
              title: "ゆるやかなプレッシャー",
              description: "コメントやステータスで適度に刺激し合い、だらけを防ぎます。",
            },
          ].map((item) => (
            <div key={item.title} className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <h3 className="text-lg font-semibold text-white">{item.title}</h3>
              <p className="mt-2 text-sm text-slate-300">{item.description}</p>
            </div>
          ))}
        </section>

        <section className="rounded-3xl border border-white/10 bg-gradient-to-br from-blue-500/20 via-slate-900 to-slate-950 p-8 shadow-2xl shadow-blue-500/30">
          <p className="text-sm uppercase tracking-[0.2em] text-blue-200">こんなチームに</p>
          <h2 className="mt-4 text-2xl font-semibold text-white">小さな開発チームのデイリーにぴったり</h2>
          <p className="mt-2 text-base text-blue-100">
            ミーティングを増やさずとも、タスク状況を共有したい。そんな気持ちをサポートするため、がんばログは常に軽く、ログイン前のこのページからでも雰囲気が伝わるように設計しています。
          </p>
        </section>
      </div>
    </main>
  );
}
