import { NextResponse } from "next/server";

const DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";

export function getBackendBaseUrl(): string {
  const baseUrl = process.env.BACKEND_BASE_URL ?? DEFAULT_BACKEND_BASE_URL;
  return baseUrl.replace(/\/$/, "");
}

type ParsedSetCookie = {
  name: string;
  value: string;
  path?: string;
  secure?: boolean;
  httpOnly?: boolean;
  sameSite?: "strict" | "lax" | "none";
  expires?: Date;
  maxAge?: number;
};

function parseSetCookieHeader(header: string): ParsedSetCookie | null {
  if (!header) {
    return null;
  }
  const parts = header.split(";").map((part) => part.trim()).filter(Boolean);
  if (parts.length === 0) {
    return null;
  }

  const [nameValue, ...attributeParts] = parts;
  const [rawName, ...rawValueParts] = nameValue.split("=");
  const name = rawName?.trim();
  if (!name) {
    return null;
  }
  const value = rawValueParts.join("=");

  const parsed: ParsedSetCookie = { name, value };

  for (const attribute of attributeParts) {
    const [attributeNameRaw, ...attributeValueParts] = attribute.split("=");
    if (!attributeNameRaw) {
      continue;
    }
    const attributeName = attributeNameRaw.trim().toLowerCase();
    const attributeValue = attributeValueParts.join("=").trim();

    switch (attributeName) {
      case "path":
        parsed.path = attributeValue || undefined;
        break;
      case "max-age": {
        const numeric = Number(attributeValue);
        if (!Number.isNaN(numeric)) {
          parsed.maxAge = numeric;
        }
        break;
      }
      case "expires": {
        const date = new Date(attributeValue);
        if (!Number.isNaN(date.getTime())) {
          parsed.expires = date;
        }
        break;
      }
      case "samesite": {
        const lower = attributeValue.toLowerCase();
        if (lower === "lax" || lower === "strict" || lower === "none") {
          parsed.sameSite = lower;
        }
        break;
      }
      case "secure":
        parsed.secure = true;
        break;
      case "httponly":
        parsed.httpOnly = true;
        break;
      default:
        break;
    }
  }

  return parsed;
}

export function applyBackendSetCookies(from: Response, to: NextResponse) {
  const getSetCookie = (from.headers as unknown as { getSetCookie?: () => string[] }).getSetCookie;
  const setCookieHeaders = getSetCookie?.call(from.headers) ?? [];

  if (setCookieHeaders.length === 0) {
    const header = from.headers.get("set-cookie");
    if (header) {
      setCookieHeaders.push(header);
    }
  }

  for (const header of setCookieHeaders) {
    const parsed = parseSetCookieHeader(header);
    if (!parsed) {
      continue;
    }
    const { name, value, path, secure, httpOnly, sameSite, expires, maxAge } = parsed;
    to.cookies.set({ name, value, path, secure, httpOnly, sameSite, expires, maxAge });
  }
}
