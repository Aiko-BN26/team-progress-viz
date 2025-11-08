import { NextRequest, NextResponse } from "next/server";

import { applyBackendSetCookies, getBackendBaseUrl } from "@/lib/backend-proxy";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  const backendBase = getBackendBaseUrl();
  const targetUrl = new URL(`/api/auth/github/callback${request.nextUrl.search}`, backendBase);

  const headers = new Headers();
  request.headers.forEach((value, key) => {
    if (key.toLowerCase() === "host") {
      return;
    }
    headers.set(key, value);
  });

  const backendResponse = await fetch(targetUrl, {
    method: "GET",
    headers,
    redirect: "manual",
    cache: "no-store",
  });

  const locationHeader = backendResponse.headers.get("location");
  const resolvedLocation = new URL(
    locationHeader ?? "/auth/callback?status=error",
    request.url,
  );

  const response = NextResponse.redirect(resolvedLocation, { status: backendResponse.status });
  applyBackendSetCookies(backendResponse, response);

  backendResponse.headers.forEach((value, key) => {
    const lowerKey = key.toLowerCase();
    if (lowerKey === "set-cookie" || lowerKey === "location") {
      return;
    }
    response.headers.set(key, value);
  });

  return response;
}
