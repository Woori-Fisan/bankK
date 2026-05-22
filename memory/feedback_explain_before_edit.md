---
name: feedback-explain-before-edit
description: 파일 수정 전에 어디를 어떻게 바꾸는지, 왜 바꾸는지 텍스트로 먼저 설명하고 진행할 것
metadata:
  type: feedback
---

파일을 수정하기 전에 반드시 텍스트로 먼저 설명하라.
- 어느 파일의 어느 부분을 바꾸는지
- 왜 바꾸는지 (의도/이유)

**Why:** 파일 수정 시 권한 확인 프롬프트가 떠서 사용자가 맥락 없이 yes/no만 눌러야 하는 상황이 불편함. 미리 설명이 있으면 무슨 변경인지 알고 확인할 수 있다.

**How to apply:** 모든 Edit/Write 도구 호출 전에 "AccountMapper.java에 findByAccountNoHash 메서드를 추가합니다 — 이유: ..." 형태로 설명 후 진행.
