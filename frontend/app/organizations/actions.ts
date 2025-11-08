"use server";

import { revalidatePath } from "next/cache";

import { backendFetchJson } from "@/lib/server-api";

interface GitHubOrganizationSummary {
  login: string;
  htmlUrl?: string | null;
}

async function registerOrganization(login: string, htmlUrl?: string | null) {
  await backendFetchJson("/api/organizations", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      login,
      defaultLinkUrl: htmlUrl ?? undefined,
    }),
  });
}

export async function refreshOrganizationsAction() {
  try {
    const { data } = await backendFetchJson<GitHubOrganizationSummary[] | null>(
      "/api/github/organizations",
    );

    const organizations = data ?? [];
    if (organizations.length === 0) {
      console.warn("[organizations] no GitHub organizations available to register");
      return;
    }

    for (const organization of organizations) {
      try {
        await registerOrganization(organization.login, organization.htmlUrl);
      } catch (error) {
        console.error("[organizations] register failed", organization.login, error);
      }
    }

    revalidatePath("/organizations");
  } catch (error) {
    console.error("[organizations] refreshOrganizationsAction failed", error);
  }
}
