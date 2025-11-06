import { useRouter } from "next/router";
import { useEffect } from "react";

export default function callback(){
  const router = useRouter();

  useEffect(() => {
    const {status, message} = router.query;

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
  },[router.query]);
}