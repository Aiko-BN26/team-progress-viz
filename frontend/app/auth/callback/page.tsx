import { useRouter } from "next/router";
import { useEffect } from "react";

export default function callback(){
  const router = useRouter();
  const {status, message} = router.query;

  useEffect(() => {
    switch (status) {
      case "success":
        console.log("ログイン成功");
        router.push("/");
        break;

      case "error":
        console.error("ログイン失敗：",message);
        router.push("/login");
        break;
      
      default:
      break;
  } 
  },[status,message]);
}