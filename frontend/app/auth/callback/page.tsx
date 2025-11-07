'use client';

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
	ActionStatus,
	API_BASE_URL,
	formatJson,
	parseSessionUser,
	readJson,
	SessionUser,
} from "@/lib/api";

type SessionCheck = {
	status: ActionStatus;
	user?: SessionUser | null;
	payload?: unknown;
	error?: string;
};

export default function AuthCallbackPage() {
	const searchParams = useSearchParams();
	const statusParam = searchParams.get("status") ?? "unknown";
	const messageParam = searchParams.get("message") ?? undefined;

	const [sessionCheck, setSessionCheck] = useState<SessionCheck>({
		status: "idle",
	});

	const refreshSession = useCallback(async () => {
		setSessionCheck({ status: "loading" });
		try {
			const response = await fetch(`${API_BASE_URL}/api/auth/session`, {
				credentials: "include",
			});

			const data = await readJson(response);
			const body = (data ?? undefined) as Record<string, unknown> | undefined;

			if (response.status === 401) {
				setSessionCheck({
					status: "success",
					user: null,
					payload: body,
				});
				return;
			}

			if (!response.ok) {
				const message =
					typeof body?.error === "string"
						? body.error
						: `Request failed (${response.status})`;
				setSessionCheck({
					status: "error",
					error: message,
					payload: body,
				});
				return;
			}

			const user = parseSessionUser(body);
			if (!user) {
				setSessionCheck({
					status: "error",
					error: "Unexpected response payload",
					payload: body,
				});
				return;
			}

			setSessionCheck({
				status: "success",
				user,
				payload: body,
			});
		} catch (error) {
			setSessionCheck({
				status: "error",
				error: error instanceof Error ? error.message : "Unknown error",
			});
		}
	}, []);

		useEffect(() => {
			// Automatically confirm the session state whenever this page mounts.
			const id = requestAnimationFrame(() => {
				void refreshSession();
			});
			return () => cancelAnimationFrame(id);
		}, [refreshSession]);

	const redirectBadge = useMemo(() => {
		if (statusParam === "success") {
			return {
				label: "status=success",
				className: "bg-emerald-100 text-emerald-700",
			};
		}

		if (statusParam === "error") {
			return {
				label: "status=error",
				className: "bg-rose-100 text-rose-700",
			};
		}

		return {
			label: `status=${statusParam}`,
			className: "bg-slate-200 text-slate-700",
		};
	}, [statusParam]);

	return (
		<div className="min-h-screen bg-slate-100 py-12">
			<main className="mx-auto flex max-w-3xl flex-col gap-8 px-4">
				<header className="space-y-3">
					<h1 className="text-3xl font-semibold text-slate-900">
						OAuth Callback Inspector
					</h1>
					<p className="text-sm text-slate-600">
						The backend redirected to this page with the query parameters shown
						below. Session data is fetched from{" "}
						<span className="font-mono">{API_BASE_URL}/api/auth/session</span>.
					</p>
				</header>

				<section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
					<div className="flex flex-wrap items-center justify-between gap-3">
						<div>
							<h2 className="text-lg font-semibold text-slate-900">
								Redirect parameters
							</h2>
							<p className="text-sm text-slate-600">
								Check whether the flow finished successfully or returned an error
								message.
							</p>
						</div>
						<span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide ${redirectBadge.className}`}>
							{redirectBadge.label}
						</span>
					</div>

					<dl className="mt-4 space-y-2 text-sm text-slate-700">
						<div className="flex flex-wrap items-center gap-2">
							<dt className="font-medium text-slate-900">status</dt>
							<dd className="font-mono text-xs">{statusParam}</dd>
						</div>
						<div className="flex flex-wrap items-center gap-2">
							<dt className="font-medium text-slate-900">message</dt>
							<dd className="font-mono text-xs">
								{messageParam ?? "(none)"}
							</dd>
						</div>
					</dl>

					<p className="mt-3 text-xs text-slate-500">
						For failures, capture the message above and compare it with the backend
						error payload.
					</p>
				</section>

				<section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
					<div className="flex flex-wrap items-center justify-between gap-3">
						<div>
							<h2 className="text-lg font-semibold text-slate-900">
								Session snapshot
							</h2>
							<p className="text-sm text-slate-600">
								Runs <code className="font-mono text-xs">GET /api/auth/session</code>{" "}
								with the current cookies.
							</p>
						</div>
						<StatusBadge status={sessionCheck.status} />
					</div>

					<div className="mt-4 flex flex-wrap items-center gap-3">
						<button
							type="button"
							onClick={refreshSession}
							disabled={sessionCheck.status === "loading"}
							className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
						>
							Refresh session
						</button>
						<Link
							href="/"
							className="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
						>
							Back to API playground
						</Link>
					</div>

					{sessionCheck.status === "success" && sessionCheck.user && (
						<div className="mt-4 flex items-center gap-4 rounded border border-emerald-200 bg-emerald-50 p-4">
							<div>
								<p className="text-sm font-semibold text-slate-900">
									{sessionCheck.user.name}
								</p>
								<p className="text-sm text-slate-600">
									{sessionCheck.user.login}
								</p>
							</div>
							<a
								href={sessionCheck.user.avatarUrl}
								target="_blank"
								rel="noreferrer"
								className="text-xs text-slate-500 underline hover:text-slate-700"
							>
								View avatar
							</a>
						</div>
					)}

					{sessionCheck.status === "success" && sessionCheck.user === null && (
						<p className="mt-3 text-sm text-slate-600">
							No authenticated user in the session (401 Unauthorized).
						</p>
					)}

					{sessionCheck.status === "error" && sessionCheck.error && (
						<p className="mt-3 text-sm text-rose-600">{sessionCheck.error}</p>
					)}

					{sessionCheck.payload !== undefined && (
						<pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
							{formatJson(sessionCheck.payload)}
						</pre>
					)}
				</section>
			</main>
		</div>
	);
}

function StatusBadge({ status }: { status: ActionStatus }) {
	const colors: Record<ActionStatus, string> = {
		idle: "bg-slate-200 text-slate-700",
		loading: "bg-blue-100 text-blue-700",
		success: "bg-emerald-100 text-emerald-700",
		error: "bg-rose-100 text-rose-700",
	};

	const labels: Record<ActionStatus, string> = {
		idle: "Idle",
		loading: "Loading",
		success: "Success",
		error: "Error",
	};

	return (
		<span
			className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide ${colors[status]}`}
		>
			{labels[status]}
		</span>
	);
}
