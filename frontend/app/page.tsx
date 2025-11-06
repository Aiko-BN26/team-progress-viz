import { useEffect, useState } from "react";
import { redirect } from "next/navigation";

import { Button } from "@/components/ui/button";

export default function Home() {
  const [isLogin, setIsLogin] = useState<boolean | null>(null);

  useEffect(() => {
    const checkIsLogin = async () => {
    try{
      const res = await fetch("http://localhost:8080/api/auth/session");
      const data = await res.json();

      // responseがemptyだった場合は未ログインと判定
      if(data.length == 0) {
        setIsLogin(false);
      } else{
        setIsLogin(true);
      }

    } catch(err){
      console.error("fetch error:", err);
    } };
    checkIsLogin();
  }, []);

  if (isLogin == false){
    redirect("/login");
  } else {

    return(
      // organization一覧
      <div className="space-y-4 p-6">
        <p className="text-lg font-semibold">organization一覧</p>
        <div className="flex flex-wrap gap-3">
          <Button>新規作成</Button>
          <Button variant="secondary">インポート</Button>
          <Button variant="outline">再読み込み</Button>
        </div>
      </div>
    );
  }
}
