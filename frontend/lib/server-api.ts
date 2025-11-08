import { cookies } from "next/headers";


async function buildCookieHeader() {
  const cookieStore = await cookies();
  const serialized = cookieStore
    .getAll()
    .map((cookie) => `${cookie.name}=${cookie.value}`)
    .join("; ");
  return serialized;
}

export async function backendFetch(path: string, init?: RequestInit) {
  const method = init?.method ?? "GET";
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");
  const cookieHeader = await buildCookieHeader();
  if (cookieHeader) {
    headers.set("Cookie", cookieHeader);
  }
  try {
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}${path}`, {
      ...init,
      headers,
      cache: "no-store",
    });
    return response;
  } catch (error) {
    console.error(`[server-api] network error ${method} ${path}`, error);
    throw error;
  }
}

export async function backendFetchJson<T>(path: string, init?: RequestInit): Promise<{
  status: number;
  data: T | null;
}> {
  const method = init?.method ?? "GET";
  const response = await backendFetch(path, init);
  if (response.status === 204) {
    return { status: response.status, data: null };
  }
  if (response.status === 404) {
    return { status: response.status, data: null };
  }
  if (!response.ok) {
    const body = await response.text();
    console.error(
      `[server-api] ${method} ${path} failed`,
      JSON.stringify({ status: response.status, body }),
    );
    throw new Error(`Failed to fetch ${path}: ${response.status}`);
  }
  const text = await response.text();
  if (!text) {
    return { status: response.status, data: null };
  }
  try {
    return { status: response.status, data: JSON.parse(text) as T };
  } catch (error) {
    console.error(`Failed to parse JSON for ${path}:`, error);
    throw error;
  }
}
