import { useState, useRef } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import './index.css'

const API_URL = '/api/proverb/stream'

export default function App() {
  const [content, setContent] = useState('')
  const [streaming, setStreaming] = useState(false)
  const [error, setError] = useState(null)
  const eventSourceRef = useRef(null)

  const startStream = () => {
    if (streaming) return

    setContent('')
    setError(null)
    setStreaming(true)

    const es = new EventSource(API_URL)
    eventSourceRef.current = es

    es.addEventListener('message', (e) => {
      setContent((prev) => prev + e.data)
    })

    es.addEventListener('done', () => {
      es.close()
      setStreaming(false)
    })

    es.addEventListener('error', (e) => {
      es.close()
      setStreaming(false)
      setError(e.data || '서버 오류가 발생했습니다.')
    })

    es.onerror = () => {
      es.close()
      setStreaming(false)
      setError('스트리밍 연결 오류가 발생했습니다.')
    }
  }

  return (
    <div className="container">
      <h1>한국어 속담 소개</h1>
      <p className="subtitle">Gemini AI가 속담을 실시간으로 스트리밍합니다.</p>

      <button
        className={`btn ${streaming ? 'btn-disabled' : 'btn-primary'}`}
        onClick={startStream}
        disabled={streaming}
      >
        {streaming ? '생성 중...' : '속담 받기'}
      </button>

      {error && <p className="error">{error}</p>}

      {content && (
        <div className="markdown-box">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
          {streaming && <span className="cursor" />}
        </div>
      )}
    </div>
  )
}
