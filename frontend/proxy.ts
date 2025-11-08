import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

const API_BASE_URL =
  process.env.API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

const PROTECTED_PREFIXES = ["/organizations", "/enroll-organizations"];
const PUBLIC_PATH_PREFIXES = ["/login", "/api", "/_next", "/static"];

export default async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  console.log("[proxy] incoming path", pathname);

  if (!isProtectedPath(pathname)) {
    console.log("[proxy] public path, skipping auth check");
    return NextResponse.next();
  }

  try {
    const sessionResponse = await fetch(`${API_BASE_URL}/api/auth/session`, {
      method: "GET",
      headers: buildHeaders(request),
      cache: "no-store",
    });

    if (sessionResponse.ok) {
      if (pathname === "/") {
        const organizationsUrl = new URL("/organizations", request.url);
        console.log("[proxy] authenticated root access, redirecting to /organizations");
        return NextResponse.redirect(organizationsUrl);
      }
      console.log("[proxy] authenticated access permitted", pathname);
      return NextResponse.next();
    }
  } catch {
    // ignore and fall through to redirect
  }

  console.warn("[proxy] authentication failed, redirecting to /login");
  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("redirect", request.nextUrl.pathname + request.nextUrl.search);
  return NextResponse.redirect(loginUrl);
}

function isProtectedPath(pathname: string) {
  if (pathname === "/") {
    return true;
  }
  if (
    PUBLIC_PATH_PREFIXES.some((prefix) =>
      pathname === prefix || pathname.startsWith(`${prefix}/`),
    )
  ) {
    return false;
  }
  return PROTECTED_PREFIXES.some((prefix) =>
    pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

function buildHeaders(request: NextRequest): Record<string, string> {
  const headers: Record<string, string> = {
    Accept: "application/json",
  };
  const cookie = request.headers.get("cookie");
  if (cookie) {
    headers.Cookie = cookie;
  }
  return headers;
}

export const config = {
  matcher: ["/", "/((?!_next/static|_next/image|favicon.ico).*)"],
};
