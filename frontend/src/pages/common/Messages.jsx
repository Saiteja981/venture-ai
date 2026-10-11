import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import DashboardLayout from "../../components/layout/DashboardLayout.jsx";
import Card from "../../components/common/Card.jsx";
import Button from "../../components/common/Button.jsx";
import Badge from "../../components/common/Badge.jsx";
import EmptyState from "../../components/common/EmptyState.jsx";
import LoadingState from "../../components/common/LoadingState.jsx";
import ErrorState from "../../components/common/ErrorState.jsx";
import { useToast } from "../../hooks/useToast.js";
import { useAuth } from "../../hooks/useAuth.js";
import { messageForError } from "../../utils/errorHandler.js";
import {
    getConversations,
    getConversationMessages,
    sendMessage,
    markConversationAsRead,
} from "../../services/messaging/messagingService.js";
import { MessageSquare, Send, ChevronLeft } from "lucide-react";

const POLL_LIST_MS = 15000;
const POLL_THREAD_MS = 5000;

function fmtTime(s) {
    if (!s) return "";
    const d = new Date(s);
    if (Number.isNaN(d.getTime())) return s;
    return d.toLocaleString("en-US", {
        month: "short", day: "2-digit",
        hour: "2-digit", minute: "2-digit",
    });
}

export default function Messages() {
    const { role, userId } = useAuth();
    const navigate = useNavigate();
    const { conversationId } = useParams();
    const toast = useToast();

    const [loadingList, setLoadingList] = useState(true);
    const [listError, setListError] = useState("");
    const [conversations, setConversations] = useState([]);

    const [loadingThread, setLoadingThread] = useState(false);
    const [threadError, setThreadError] = useState("");
    const [messages, setMessages] = useState([]);
    const [draft, setDraft] = useState("");
    const [sending, setSending] = useState(false);
    const messagesEndRef = useRef(null);

    const basePath = role === "INVESTOR" ? "/investor/messages" : "/startup/messages";

    const loadConversations = useCallback(async () => {
        try {
            const data = await getConversations();
            setConversations(Array.isArray(data) ? data : []);
            setListError("");
        } catch (err) {
            setListError(messageForError(err));
        } finally {
            setLoadingList(false);
        }
    }, []);

    useEffect(() => {
        loadConversations();
        const t = setInterval(loadConversations, POLL_LIST_MS);
        return () => clearInterval(t);
    }, [loadConversations]);

    const loadThread = useCallback(async (id) => {
        try {
            const data = await getConversationMessages(id);
            setMessages(Array.isArray(data) ? data : []);
            setThreadError("");
            await markConversationAsRead(id);
        } catch (err) {
            setThreadError(messageForError(err));
        } finally {
            setLoadingThread(false);
        }
    }, []);

    useEffect(() => {
        if (!conversationId) return;
        setLoadingThread(true);
        loadThread(conversationId);

        const t = setInterval(() => loadThread(conversationId), POLL_THREAD_MS);
        return () => clearInterval(t);
    }, [conversationId, loadThread]);

    useEffect(() => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages]);

    async function handleSend(e) {
        if (e) e.preventDefault();
        const content = draft.trim();
        if (!content || sending) return;
        setSending(true);
        try {
            const created = await sendMessage(conversationId, content);
            setMessages((prev) => [...prev, created]);
            setDraft("");
            loadConversations();
        } catch (err) {
            toast.error(messageForError(err));
        } finally {
            setSending(false);
        }
    }

    const activeConv = useMemo(
        () => conversations.find((c) => String(c.id) === String(conversationId)),
        [conversations, conversationId]
    );

    return (
        <DashboardLayout title="Messages">
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                {/* Conversation list */}
                <div className={`lg:col-span-1 ${conversationId ? "hidden lg:block" : ""}`}>
                    <Card className="!p-0">
                        <div className="border-b border-slate-200 px-4 py-3">
                            <h2 className="text-base font-semibold text-navy-800">Conversations</h2>
                        </div>

                        {loadingList ? (
                            <div className="p-4">
                                <LoadingState message="Loading conversations…" />
                            </div>
                        ) : listError ? (
                            <div className="p-4">
                                <ErrorState
                                    title="Could not load"
                                    message={listError}
                                    action={<Button onClick={loadConversations}>Retry</Button>}
                                />
                            </div>
                        ) : conversations.length === 0 ? (
                            <div className="p-6 text-center text-sm text-slate-500">
                                No active conversations yet.
                                <br />
                                Conversations begin after a collaboration is accepted.
                            </div>
                        ) : (
                            <ul className="divide-y divide-slate-100">
                                {conversations.map((c) => {
                                    const active = String(c.id) === String(conversationId);
                                    return (
                                        <li key={c.id}>
                                            <button
                                                type="button"
                                                onClick={() => navigate(`${basePath}/${c.id}`)}
                                                className={`w-full px-4 py-3 text-left hover:bg-slate-50 ${active ? "bg-brand-50/50" : ""}`}
                                            >
                                                <div className="flex items-center justify-between gap-2">
                                                    <span className="truncate text-sm font-semibold text-navy-800">
                                                        {role === "INVESTOR" ? c.startupName : c.investorName}
                                                    </span>
                                                    {c.unreadCount > 0 && (
                                                        <Badge tone="brand">{c.unreadCount}</Badge>
                                                    )}
                                                </div>
                                                <div className="mt-0.5 truncate text-xs text-slate-500">
                                                    {c.lastMessagePreview || "No messages yet"}
                                                </div>
                                                <div className="mt-1 text-[11px] text-slate-400">
                                                    {fmtTime(c.lastMessageAt)}
                                                </div>
                                            </button>
                                        </li>
                                    );
                                })}
                            </ul>
                        )}
                    </Card>
                </div>

                {/* Thread */}
                <div className={`lg:col-span-2 ${conversationId ? "" : "hidden lg:block"}`}>
                    {!conversationId ? (
                        <EmptyState
                            icon={MessageSquare}
                            title="Select a conversation"
                            description="Choose a conversation from the list to start messaging."
                        />
                    ) : loadingThread ? (
                        <LoadingState message="Loading messages…" />
                    ) : threadError ? (
                        <ErrorState
                            title="Could not load messages"
                            message={threadError}
                            action={<Button onClick={() => loadThread(conversationId)}>Retry</Button>}
                        />
                    ) : (
                        <Card className="!p-0 flex flex-col" style={{ height: "70vh" }}>
                            <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-3">
                                <button
                                    type="button"
                                    className="rounded p-1 text-slate-500 hover:bg-slate-100 lg:hidden"
                                    onClick={() => navigate(basePath)}
                                    aria-label="Back"
                                >
                                    <ChevronLeft className="h-4 w-4" />
                                </button>
                                <h2 className="text-base font-semibold text-navy-800">
                                    {role === "INVESTOR"
                                        ? (activeConv?.startupName ?? "Conversation")
                                        : (activeConv?.investorName ?? "Conversation")}
                                </h2>
                            </div>

                            <div className="flex-1 space-y-3 overflow-y-auto bg-slate-50 px-4 py-4">
                                {messages.length === 0 ? (
                                    <p className="py-8 text-center text-sm text-slate-500">
                                        No messages yet. Say hello!
                                    </p>
                                ) : (
                                    messages.map((m) => {
                                        const mine = String(m.senderId) === String(userId);
                                        return (
                                            <div
                                                key={m.id}
                                                className={`flex ${mine ? "justify-end" : "justify-start"}`}
                                            >
                                                <div
                                                    className={`max-w-[75%] rounded-2xl px-3.5 py-2 text-sm ${
                                                        mine
                                                            ? "bg-brand-600 text-white"
                                                            : "border border-slate-200 bg-white text-navy-800"
                                                    }`}
                                                >
                                                    {!mine && (
                                                        <div className="mb-0.5 text-[11px] font-medium text-slate-500">
                                                            {m.senderName}
                                                        </div>
                                                    )}
                                                    <div className="whitespace-pre-wrap break-words">
                                                        {m.content}
                                                    </div>
                                                    <div
                                                        className={`mt-1 text-[10px] ${
                                                            mine ? "text-brand-100" : "text-slate-400"
                                                        }`}
                                                    >
                                                        {fmtTime(m.createdAt)}
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })
                                )}
                                <div ref={messagesEndRef} />
                            </div>

                            <form onSubmit={handleSend} className="border-t border-slate-200 px-3 py-3">
                                <div className="flex items-end gap-2">
                                    <textarea
                                        rows={2}
                                        value={draft}
                                        onChange={(e) => setDraft(e.target.value)}
                                        maxLength={4000}
                                        placeholder="Type a message…"
                                        className="input-field flex-1 resize-none"
                                        onKeyDown={(e) => {
                                            if (e.key === "Enter" && !e.shiftKey) {
                                                e.preventDefault();
                                                handleSend(e);
                                            }
                                        }}
                                    />
                                    <Button type="submit" disabled={sending || !draft.trim()}>
                                        <Send className="h-4 w-4" />
                                        {sending ? "Sending…" : "Send"}
                                    </Button>
                                </div>
                            </form>
                        </Card>
                    )}
                </div>
            </div>
        </DashboardLayout>
    );
}