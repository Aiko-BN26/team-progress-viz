"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function LoginPage() {
  const router = useRouter();

  useEffect(() => {
    const checkIsLogin = async () => {
      try {
        const res = await fetch("https://team-progress-viz.onrender.com/api/auth/github/login", {
          credentials: "include",
        });
        const data = await res.json();

        switch (res.status) {
          case 200: {
            const authorizationUrl = data.authorizationUrl;
            if (authorizationUrl) {
              router.push(authorizationUrl);
            }
            break;
          }
          case 500:
            console.error(data.error);
            break;
          default:
            break;
        }
      } catch (err) {
        console.error("fetch error:", err);
      }
    };

    checkIsLogin();
  }, [router]);

  return null;
}
