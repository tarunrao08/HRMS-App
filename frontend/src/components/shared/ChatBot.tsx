import { useState, useRef, useEffect } from "react"
import { MessageCircle, X, Send, Bot, User } from "lucide-react"
import { Button } from "@/components/ui/button"
import api from "@/services/api"

interface Message {
  id: number
  role: "user" | "bot"
  text: string
}

let msgId = 0

export default function ChatBot() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([
    { id: ++msgId, role: "bot", text: "Hi! I'm your HRMS Assistant. Type \"help\" to see what I can do." },
  ])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (open) {
      bottomRef.current?.scrollIntoView({ behavior: "smooth" })
      setTimeout(() => inputRef.current?.focus(), 100)
    }
  }, [open, messages])

  async function send() {
    const text = input.trim()
    if (!text || loading) return

    setInput("")
    setMessages((prev) => [...prev, { id: ++msgId, role: "user", text }])
    setLoading(true)

    try {
      const res = await api.post("/bot/chat", { message: text })
      const reply: string = res.data?.data?.reply ?? res.data?.reply ?? "Sorry, I couldn't get a response."
      setMessages((prev) => [...prev, { id: ++msgId, role: "bot", text: reply }])
    } catch {
      setMessages((prev) => [
        ...prev,
        { id: ++msgId, role: "bot", text: "Something went wrong. Please try again." },
      ])
    } finally {
      setLoading(false)
    }
  }

  function onKey(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  return (
    <>
      {/* Chat window */}
      {open && (
        <div className="fixed bottom-20 right-6 z-50 flex flex-col w-[360px] h-[500px] rounded-xl shadow-2xl border border-gray-200 bg-white overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-primary text-white flex-shrink-0">
            <div className="flex items-center gap-2">
              <Bot className="w-5 h-5" />
              <span className="font-semibold text-sm">HRMS Assistant</span>
            </div>
            <button
              onClick={() => setOpen(false)}
              className="text-white/80 hover:text-white transition-colors"
              aria-label="Close chat"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-3 py-3 space-y-3 bg-gray-50">
            {messages.map((m) => (
              <div key={m.id} className={`flex gap-2 ${m.role === "user" ? "flex-row-reverse" : "flex-row"}`}>
                <div
                  className={`flex-shrink-0 w-7 h-7 rounded-full flex items-center justify-center ${
                    m.role === "user" ? "bg-primary text-white" : "bg-gray-200 text-gray-600"
                  }`}
                >
                  {m.role === "user" ? <User className="w-3.5 h-3.5" /> : <Bot className="w-3.5 h-3.5" />}
                </div>
                <div
                  className={`max-w-[75%] px-3 py-2 rounded-2xl text-sm whitespace-pre-wrap leading-relaxed ${
                    m.role === "user"
                      ? "bg-primary text-white rounded-tr-sm"
                      : "bg-white text-gray-800 rounded-tl-sm shadow-sm border border-gray-100"
                  }`}
                >
                  {m.text}
                </div>
              </div>
            ))}

            {loading && (
              <div className="flex gap-2 flex-row">
                <div className="flex-shrink-0 w-7 h-7 rounded-full flex items-center justify-center bg-gray-200 text-gray-600">
                  <Bot className="w-3.5 h-3.5" />
                </div>
                <div className="bg-white border border-gray-100 shadow-sm rounded-2xl rounded-tl-sm px-4 py-2.5">
                  <div className="flex gap-1 items-center">
                    <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:0ms]" />
                    <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:150ms]" />
                    <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:300ms]" />
                  </div>
                </div>
              </div>
            )}

            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div className="flex items-center gap-2 px-3 py-2.5 border-t border-gray-100 bg-white flex-shrink-0">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKey}
              placeholder="Ask me anything…"
              disabled={loading}
              className="flex-1 text-sm outline-none bg-transparent placeholder:text-gray-400 disabled:opacity-50"
            />
            <Button
              size="icon"
              onClick={send}
              disabled={!input.trim() || loading}
              className="w-8 h-8 rounded-full flex-shrink-0"
            >
              <Send className="w-3.5 h-3.5" />
            </Button>
          </div>
        </div>
      )}

      {/* Toggle bubble */}
      <button
        onClick={() => setOpen((v) => !v)}
        className="fixed bottom-6 right-6 z-50 w-13 h-13 rounded-full bg-primary text-white shadow-lg hover:shadow-xl hover:scale-105 active:scale-95 transition-all duration-200 flex items-center justify-center"
        aria-label={open ? "Close chat" : "Open HRMS Assistant"}
        style={{ width: 52, height: 52 }}
      >
        {open ? <X className="w-5 h-5" /> : <MessageCircle className="w-5 h-5" />}
      </button>
    </>
  )
}
