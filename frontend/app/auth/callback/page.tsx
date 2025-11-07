"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export default function CallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const status = searchParams.get("status");
  const message = searchParams.get("message");

  useEffect(() => {
    switch (status) {
      case "success":
        console.log("ログイン成功");
        router.push("/organizations");
        break;
      case "error":
        console.error("ログイン失敗：", message);
        router.push("/login");
        break;
      default:
        break;
    }
  }, [router, status, message]);

  return null;
}
