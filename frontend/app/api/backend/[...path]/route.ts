import { NextRequest, NextResponse } from "next/server";

import { applyBackendSetCookies, getBackendBaseUrl } from "@/lib/backend-proxy";

export const dynamic = "force-dynamic";

const hopByHopHeaders = new Set([
  "connection",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
]);

function buildBackendUrl(pathSegments: string[] | undefined, search: string): URL {
  const base = getBackendBaseUrl();
  const joinedPath = pathSegments?.length ? `/${pathSegments.join("/")}` : "";
  return new URL(`${joinedPath}${search}`, base);
}

function filterRequestHeaders(original: Headers): Headers {
  const headers = new Headers();
  original.forEach((value, key) => {
    const lowerKey = key.toLowerCase();
    if (lowerKey === "host" || hopByHopHeaders.has(lowerKey)) {
      return;
    }
    headers.set(key, value);
  });
  return headers;
}

function filterResponseHeaders(original: Headers): Headers {
  const headers = new Headers();
  original.forEach((value, key) => {
    const lowerKey = key.toLowerCase();
    if (lowerKey === "set-cookie" || hopByHopHeaders.has(lowerKey)) {
      return;
    }
    headers.append(key, value);
  });
  return headers;
}

type RouteParams = { path?: string[] };

async function handle(
  request: NextRequest,
  context: { params: RouteParams } | { params: Promise<RouteParams> },
): Promise<NextResponse> {
  const maybePromise = context.params as RouteParams | Promise<RouteParams> | undefined;
  const params: RouteParams =
    maybePromise && typeof (maybePromise as PromiseLike<RouteParams>).then === "function"
      ? await (maybePromise as Promise<RouteParams>)
      : (maybePromise as RouteParams | undefined) ?? {};
  const targetUrl = buildBackendUrl(params.path, request.nextUrl.search);
  const headers = filterRequestHeaders(request.headers);
  if (request.nextUrl.pathname.includes("/api/auth/session")) {
    console.log("[backend-proxy] forwarding cookies", headers.get("cookie"));
  }

  const hasBody = request.method !== "GET" && request.method !== "HEAD";

  const init: RequestInit = {
    method: request.method,
    headers,
    redirect: "manual",
    body: hasBody ? request.body : undefined,
    cache: "no-store",
  };

  if (hasBody && request.body) {
    // @ts-expect-error Node.js streaming flag for fetch
    init.duplex = "half";
  }

  const backendResponse = await fetch(targetUrl, init);
  const filteredHeaders = filterResponseHeaders(backendResponse.headers);
  const response = new NextResponse(backendResponse.body, {
    status: backendResponse.status,
    headers: filteredHeaders,
  });

  applyBackendSetCookies(backendResponse, response);

  const location = backendResponse.headers.get("location");
  if (location) {
    response.headers.set("location", location);
  }

  return response;
}

export const GET = handle;
export const HEAD = handle;
export const POST = handle;
export const PUT = handle;
export const PATCH = handle;
export const DELETE = handle;
export const OPTIONS = handle;
