"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { isRedirectError } from "next/dist/client/components/redirect-error";

import { backendFetchJson } from "@/lib/server-api";

type ApiOrganizationSummary = {
  id: number;
};

type OrganizationRegistrationResponse = {
  organizationId?: number;
};

type JobSubmissionResponse = {
  jobId?: string;
  status?: string;
};

type JobSyncStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED";

type JobStatusResponse = {
  jobId: string;
  status: JobSyncStatus;
  progress?: number | null;
  errorMessage?: string | null;
};

export type SyncOrganizationActionState = {
  ok: boolean;
  message?: string;
};

const JOB_POLL_INTERVAL_MS = 1000;
const JOB_POLL_TIMEOUT_MS = 60_000;

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function triggerOrganizationSync(organizationId: number) {
  const result = await backendFetchJson<JobSubmissionResponse>(`/api/organizations/${organizationId}/sync`, {
    method: "POST",
  });
  return result.data ?? null;
}

async function waitForJobCompletion(jobId: string) {
  let deadline = Date.now() + JOB_POLL_TIMEOUT_MS;
  let lastProgress = -1;
  while (Date.now() < deadline) {
    await delay(JOB_POLL_INTERVAL_MS);
    const statusResult = await backendFetchJson<JobStatusResponse>(`/api/jobs/${jobId}`);
    if (statusResult.status === 404 || !statusResult.data) {
      throw new Error(`ジョブ(${jobId})の状態を取得できませんでした`);
    }
    const status = statusResult.data.status;
    const progress = statusResult.data.progress ?? null;
    if (typeof progress === "number" && Number.isFinite(progress)) {
      if (progress > lastProgress) {
        lastProgress = progress;
        if (progress < 100) {
          deadline = Date.now() + JOB_POLL_TIMEOUT_MS;
        }
      }
    }
    if (status === "SUCCEEDED") {
      return;
    }
    if (status === "FAILED") {
      throw new Error(statusResult.data.errorMessage ?? "同期ジョブが失敗しました");
    }
  }
  throw new Error("同期ジョブがタイムアウトしました");
}

const defaultSyncErrorMessage = "同期に失敗しました。時間をおいて再度お試しください。";

export async function syncOrganizationAction(
  organizationIdInput: number | string,
  _prevState: SyncOrganizationActionState,
  _formData?: FormData,
): Promise<SyncOrganizationActionState> {
  void _prevState;
  void _formData;

  try {
    const syncId = normalizeOrganizationId(organizationIdInput);
    if (syncId == null) {
      throw new Error("同期対象の組織IDが不正です");
    }

    const submission = await triggerOrganizationSync(syncId);
    if (submission?.jobId) {
      await waitForJobCompletion(submission.jobId);
    }
    const pathId = organizationIdInput.toString();
    revalidatePath(`/organizations/${pathId}`);
    revalidatePath("/organizations");
    redirect(`/organizations/${pathId}`);
  } catch (error) {
    if (isRedirectError(error)) {
      throw error;
    }
    console.error("[organizations] syncOrganizationAction failed", organizationIdInput, error);
    const message = error instanceof Error ? error.message : defaultSyncErrorMessage;
    return {
      ok: false,
      message,
    };
  }

  return {
    ok: true,
  };
}

function normalizeOrganizationId(value: number | string): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number.parseInt(value, 10);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return null;
}

export type RegisterOrganizationResult = {
  ok: boolean;
  message?: string;
};

export async function refreshOrganizationsAction() {
  try {
    const summariesResult = await backendFetchJson<ApiOrganizationSummary[]>("/api/organizations");
    const organizationIds = summariesResult.data
      ?.map((summary) => summary.id)
      .filter((id): id is number => typeof id === "number" && Number.isFinite(id));

    if (!organizationIds || organizationIds.length === 0) {
      console.warn("[organizations] No organizations found for sync");
    } else {
      const syncResults = await Promise.allSettled(
        organizationIds.map((organizationId) => triggerOrganizationSync(organizationId)),
      );
      syncResults.forEach((result, index) => {
        if (result.status === "rejected") {
          console.error(
            "[organizations] Failed to trigger sync",
            organizationIds[index],
            result.reason,
          );
        }
      });
    }

    revalidatePath("/organizations");
  } catch (error) {
    console.error("[organizations] refreshOrganizationsAction failed", error);
  }
}

export async function registerOrganizationAction(
  input: { login: string },
): Promise<RegisterOrganizationResult> {
  const login = input.login?.trim();
  if (!login) {
    return {
      ok: false,
      message: "組織スラッグを入力してください",
    };
  }

  try {
    const registrationResult = await backendFetchJson<OrganizationRegistrationResponse>("/api/organizations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ login }),
    });

    const organizationId = registrationResult.data?.organizationId;
    if (organizationId != null) {
      try {
        await triggerOrganizationSync(organizationId);
      } catch (syncError) {
        console.error(
          "[organizations] registerOrganizationAction failed to trigger sync",
          organizationId,
          syncError,
        );
      }
    }

    revalidatePath("/organizations");

    return {
      ok: true,
    };
  } catch (error) {
    console.error("[organizations] registerOrganizationAction failed", error);
    return {
      ok: false,
      message: "登録に失敗しました。時間をおいて再度お試しください。",
    };
  }
}
