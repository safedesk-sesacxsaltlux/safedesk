"use client";

// GET API 를 부르고 결과를 상태로 돌려주는 훅. 경로나 파라미터가 바뀌면 다시 부른다.
//
// effect 안에서 setState("loading")를 바로 부르지 않도록(react-hooks/set-state-in-effect),
// 응답에 "어떤 요청의 결과인지(key)"를 같이 저장하고, 지금 요청과 key 가 다르면 loading 으로 본다.
//
// 반환: { status: "loading" | "ready" | "error", data, errorCode, message, previous }
//  - previous: 새 요청을 기다리는 동안 보여 줄 직전 결과 (기간 탭을 바꿀 때 화면이 비지 않게)

import { useEffect, useState } from "react";
import { apiGet } from "./api";

export function useApiGet(path, params) {
  const paramsJson = JSON.stringify(params ?? {});
  const key = `${path}?${paramsJson}`;
  const [result, setResult] = useState(null);

  useEffect(() => {
    let cancelled = false;
    apiGet(path, JSON.parse(paramsJson)).then((r) => {
      if (!cancelled) setResult({ key: `${path}?${paramsJson}`, ...r });
    });
    return () => {
      cancelled = true;
    };
  }, [path, paramsJson]);

  if (!result || result.key !== key) {
    return { status: "loading", previous: result?.ok ? result.data : null };
  }
  if (!result.ok) {
    return { status: "error", errorCode: result.errorCode, message: result.message };
  }
  return { status: "ready", data: result.data, previous: result.data };
}
