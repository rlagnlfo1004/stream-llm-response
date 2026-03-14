# stream-llm-response

Gemini LLM의 응답을 청크 단위로 실시간 마크다운 렌더링하는 스트리밍 데모 서비스.

## 구조

```
streaming-demo/
├── server/   # Spring Boot 백엔드
├── web/      # React 프론트엔드
└── docker-compose.yml
```

## 기술 스택

### Backend
| 기술 | 역할 |
|------|------|
| **Spring Boot 4.0** | 웹 프레임워크 |
| **Spring MVC + SseEmitter** | SSE 스트리밍 응답 |
| **RestClient** | Gemini API HTTP 호출 |
| **Java 21** | 텍스트 블록, 레코드 등 최신 문법 활용 |

### Frontend
| 기술 | 역할 |
|------|------|
| **React 19** | UI 프레임워크 |
| **EventSource API** | SSE 수신 (브라우저 내장) |
| **react-markdown + remark-gfm** | 실시간 마크다운 렌더링 |
| **Vite** | 빌드 도구 / 개발 서버 |

### Infrastructure
| 기술 | 역할 |
|------|------|
| **Docker + Docker Compose** | 컨테이너화 및 오케스트레이션 |
| **nginx** | React 정적 파일 서빙 + `/api` 역방향 프록시 |
| **Gemini API (`gemini-2.0-flash`)** | LLM 스트리밍 응답 |

### 프로토콜 선택: SSE (Server-Sent Events)
서버 → 클라이언트 **단방향 텍스트 스트리밍**만 필요하므로 WebSocket 대신 SSE를 선택.
- HTTP 위에서 동작해 별도 프로토콜 업그레이드 불필요
- 브라우저 내장 `EventSource`로 클라이언트 구현 단순화
- Gemini API 자체도 `?alt=sse` 파라미터로 SSE 형식 지원

## 데이터 흐름

```
Browser (EventSource)
   │  GET /api/proverb/stream
   ▼
nginx (port 3000)
   │  proxy_pass http://backend:8080
   ▼
Spring Boot (SseEmitter)
   │  POST streamGenerateContent?alt=sse
   ▼
Gemini API
```

## 실행 방법

### Docker (권장)

```bash
# 1. Gemini API 키 설정
export GEMINI_API_KEY=your-api-key-here

# 2. 빌드 및 실행
docker compose up --build
```

브라우저에서 http://localhost:3000 접속

### 로컬 개발

```bash
# 터미널 1 — 백엔드 (server/)
export GEMINI_API_KEY=your-api-key-here
./gradlew bootRun

# 터미널 2 — 프론트엔드 (web/)
npm install
npm run dev
```

브라우저에서 http://localhost:5173 접속
(Vite가 `/api` 요청을 `localhost:8080`으로 자동 프록시)

## Gemini API 키 발급

[Google AI Studio](https://aistudio.google.com/app/apikey)에서 무료로 발급 가능.
