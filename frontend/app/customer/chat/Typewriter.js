"use client";

import { useEffect, useState } from "react";

const TICK_MS = 30;
// 긴 답변도 이 시간 안에 다 보이게 한 번에 여러 글자씩 늘린다
const MAX_DURATION_MS = 2_000;

// 완성된 답변을 한 글자씩 보여준다.
// CUS-02 명세: 1차는 SSE 대신 완성된 답변을 받고 화면에서 순차 출력한다.
export default function Typewriter({ text, onTick, onDone }) {
  const [shown, setShown] = useState(0);

  useEffect(() => {
    const step = Math.max(1, Math.ceil(text.length / (MAX_DURATION_MS / TICK_MS)));
    const timer = setInterval(() => {
      setShown((prev) => Math.min(prev + step, text.length));
    }, TICK_MS);
    return () => clearInterval(timer);
  }, [text]);

  const done = shown >= text.length;

  useEffect(() => {
    onTick?.();
    if (done) onDone?.();
    // onTick·onDone 은 부모가 매번 새로 만들어도 글자 수가 바뀔 때만 부른다
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shown, done]);

  return (
    <>
      {text.slice(0, shown)}
      {!done && <span className="cursor" />}
    </>
  );
}
