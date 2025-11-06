import { useEffect, useState } from "react";
import router from "next/router";
import { redirect } from "next/navigation";

export default function Home() {
  const [isLogin, setIsLogin] = useState<boolean | null>(null);

  useEffect(() => {
    const checkIsLogin = async () => {
    try{
      const res = await fetch("http:localhost:8080/api/auth/session");
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
      <div>
        <p>organization一覧</p>
      </div>
    );
  }
}
