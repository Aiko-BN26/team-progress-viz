"use client";

import { useEffect, useState } from "react";
import { redirect } from "next/navigation";

const baseUrl = process.env.NEXT_PUBLIC_API_URL;
if (!baseUrl) {
  throw new Error("NEXT_PUBLIC_API_URL is not defined");
}

export default function Home() {
  const [isLogin, setIsLogin] = useState<boolean | null>(null);

  useEffect(() => {
    const checkIsLogin = async () => {
    try{
      const res = await fetch(`${baseUrl}/api/auth/session`, {credentials: "include"});
      const bodyText = await res.text();
      const data = bodyText ? JSON.parse(bodyText) : null;

      if(!data || (Array.isArray(data) && data.length === 0)) {
        setIsLogin(false);
      } else{
        setIsLogin(true);
      }

    } catch(err){
      console.error("fetch error:", err);
      setIsLogin(false);
    } };
    checkIsLogin();
  }, []);

  if (isLogin == false){
    redirect("/login");
  } else {

    return(
      <div className="space-y-4 p-6">
        GitHubのアカウントにてログインします...
      </div>
    );
  }
}
