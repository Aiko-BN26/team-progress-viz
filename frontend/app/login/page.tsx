import { redirect } from "next/navigation";
import { useEffect } from "react";

export default function login() {
  useEffect(() => {
      const checkIsLogin = async () => {
      try{
        const res = await fetch("/api/auth/github/login");
        const data = await res.json();

        switch(res.status){
          case 200:
            const authorizationUrl = data.authorizationUrl;
            redirect(authorizationUrl);
            break;

          case 500:
            console.error(data.error);
            break;

          default:
            break;
        }
      } catch(err){
        console.error("fetch error:",err);
      } };
      checkIsLogin();
    }, []);
}