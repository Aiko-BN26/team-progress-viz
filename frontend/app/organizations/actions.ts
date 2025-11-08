"use server";

import { revalidatePath } from "next/cache";

import { backendFetchJson } from "@/lib/server-api";

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
