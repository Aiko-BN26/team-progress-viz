"use server";

import { revalidatePath } from "next/cache";

import { backendFetchJson } from "@/lib/server-api";

type ApiOrganizationSummary = {
  id: number;
};

type OrganizationRegistrationResponse = {
  organizationId?: number;
};

async function triggerOrganizationSync(organizationId: number) {
  await backendFetchJson(`/api/organizations/${organizationId}/sync`, {
    method: "POST",
  });
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
