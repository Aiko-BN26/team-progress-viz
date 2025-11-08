"use server";

import { revalidatePath } from "next/cache";

import { backendFetchJson } from "@/lib/server-api";

export type RegisterOrganizationResult = {
  ok: boolean;
  message?: string;
};

export async function refreshOrganizationsAction() {
  try {
    await backendFetchJson("/api/organizations", {
      method: "POST",
    });

    await backendFetchJson("/api/organizations");

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
    await backendFetchJson("/api/organizations", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ login }),
    });

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
