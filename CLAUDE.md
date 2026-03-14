# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SSE(Server-Sent Events) 기반으로 Gemini LLM의 응답을 실시간 마크다운 스트리밍으로 보여주는 데모 서비스.
- **`server/`**: Spring Boot 4.0.3 + Java 21 백엔드 (Spring MVC, SSE)
- **`web/`**: React 19 + Vite 프론트엔드 (react-markdown, EventSource)

## Commands

### Backend (`server/` 디렉토리에서 실행)
```bash
./gradlew bootRun          # 서버 실행 (포트 8080)
./gradlew build            # 빌드
./gradlew test             # 전체 테스트
./gradlew test --tests "com.mailsangja.streamingdemo.SomeTest"  # 단일 테스트
./gradlew clean            # 빌드 결과물 삭제
```

### Frontend (`web/` 디렉토리에서 실행)
```bash
npm install    # 의존성 설치 (최초 1회)
npm run dev    # 개발 서버 실행 (포트 5173)
npm run build  # 프로덕션 빌드
```

### Gemini API 키 설정
```bash
export GEMINI_API_KEY=your-api-key-here
```
또는 `server/src/main/resources/application.yaml`에 직접 입력.

## Architecture

### 데이터 흐름
```
React (EventSource) → SSE → Spring (SseEmitter) → Gemini API (?alt=sse)
```

### Backend
- **`ProverbController`**: `GET /api/proverb/stream` — SseEmitter 반환, CompletableFuture로 비동기 실행
- **`GeminiService`**: Gemini `streamGenerateContent` API를 `?alt=sse` 파라미터로 호출 → 응답 라인을 파싱하여 `data: {json}` 형식에서 텍스트 추출 → SseEmitter로 청크 전송 → 완료 시 `done` 이벤트 전송
- **`WebConfig`**: CORS 설정 (`localhost:5173` 허용)
- SSE 이벤트 타입: `message` (텍스트 청크), `done` (스트림 종료 신호)

### Frontend
- `App.jsx`: EventSource로 SSE 연결, `message` 이벤트로 텍스트 누적, `done` 이벤트로 연결 종료
- `react-markdown` + `remark-gfm`으로 누적된 마크다운을 실시간 렌더링
- 스트리밍 중 커서 애니메이션 표시

### Protocol 선택 이유 (SSE vs WebSocket)
서버→클라이언트 단방향 텍스트 전송만 필요하므로 WebSocket보다 단순한 SSE 사용.
