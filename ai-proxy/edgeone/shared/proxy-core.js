const ZHIPU_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
const MODEL_NAME = "glm-4.7-flash";
const CLIENT_TOKEN_HEADER = "X-SilverGuard-Client-Token";
const MAX_REQUEST_BYTES = 24_000;
const MAX_TOTAL_MESSAGE_CHARS = 12_000;
const MAX_RESPONSE_CHARS = 80_000;
const UPSTREAM_TIMEOUT_MS = 50_000;

export function handleHealthRequest(env) {
  const runtimeEnv = resolveRuntimeEnv(env);
  return jsonResponse({
    status: "ok",
    model: MODEL_NAME,
    service: "edgeone",
    configured: Boolean(runtimeEnv.ZHIPU_API_KEY && runtimeEnv.SILVERGUARD_APP_TOKEN),
  });
}

export async function handleChatRequest(request, env) {
  const runtimeEnv = resolveRuntimeEnv(env);
  if (!runtimeEnv.ZHIPU_API_KEY || !runtimeEnv.SILVERGUARD_APP_TOKEN) {
    return jsonResponse({ error: "proxy_not_configured" }, 503);
  }

  const suppliedToken = request.headers.get(CLIENT_TOKEN_HEADER) || "";
  if (!(await secureTokenEquals(suppliedToken, runtimeEnv.SILVERGUARD_APP_TOKEN))) {
    return jsonResponse({ error: "unauthorized" }, 401);
  }

  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength > MAX_REQUEST_BYTES) {
    return jsonResponse({ error: "request_too_large" }, 413);
  }

  let incoming;
  try {
    const rawBody = await request.text();
    if (new TextEncoder().encode(rawBody).byteLength > MAX_REQUEST_BYTES) {
      return jsonResponse({ error: "request_too_large" }, 413);
    }
    incoming = JSON.parse(rawBody);
  } catch {
    return jsonResponse({ error: "invalid_json" }, 400);
  }

  const messages = validateMessages(incoming?.messages);
  if (!messages) return jsonResponse({ error: "invalid_messages" }, 400);

  const upstreamBody = {
    model: MODEL_NAME,
    messages,
    temperature: 0.2,
    max_tokens: 900,
    stream: false,
    response_format: { type: "json_object" },
  };

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), UPSTREAM_TIMEOUT_MS);
  let upstream;
  try {
    upstream = await fetch(ZHIPU_ENDPOINT, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${runtimeEnv.ZHIPU_API_KEY}`,
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
      body: JSON.stringify(upstreamBody),
      signal: controller.signal,
    });
  } catch {
    return jsonResponse({ error: "upstream_unavailable" }, 503);
  } finally {
    clearTimeout(timeout);
  }

  const responseText = await upstream.text();
  if (responseText.length > MAX_RESPONSE_CHARS) {
    return jsonResponse({ error: "upstream_response_too_large" }, 502);
  }
  if (!upstream.ok) {
    const status = upstream.status === 429 ? 429 : upstream.status >= 500 ? 503 : 502;
    return jsonResponse({ error: "upstream_error" }, status);
  }

  return new Response(responseText, { status: 200, headers: responseHeaders() });
}

export function resolveRuntimeEnv(contextEnv) {
  const globalEnv = typeof globalThis.env === "object" && globalThis.env
    ? globalThis.env
    : {};
  const nodeEnv = typeof process !== "undefined" && process?.env
    ? process.env
    : {};
  return {
    ZHIPU_API_KEY:
      contextEnv?.ZHIPU_API_KEY || globalEnv.ZHIPU_API_KEY || nodeEnv.ZHIPU_API_KEY || "",
    SILVERGUARD_APP_TOKEN:
      contextEnv?.SILVERGUARD_APP_TOKEN ||
      globalEnv.SILVERGUARD_APP_TOKEN ||
      nodeEnv.SILVERGUARD_APP_TOKEN ||
      "",
  };
}

export function validateMessages(messages) {
  if (!Array.isArray(messages) || messages.length < 1 || messages.length > 3) return null;
  let totalChars = 0;
  const clean = [];
  for (const message of messages) {
    if (!message || !["system", "user"].includes(message.role)) return null;
    if (typeof message.content !== "string" || message.content.length < 1) return null;
    totalChars += message.content.length;
    if (totalChars > MAX_TOTAL_MESSAGE_CHARS) return null;
    clean.push({ role: message.role, content: message.content });
  }
  if (!clean.some((message) => message.role === "user")) return null;
  return clean;
}

async function secureTokenEquals(left, right) {
  if (!left || !right) return false;
  const [leftHash, rightHash] = await Promise.all([sha256Hex(left), sha256Hex(right)]);
  let difference = leftHash.length ^ rightHash.length;
  const maxLength = Math.max(leftHash.length, rightHash.length);
  for (let index = 0; index < maxLength; index += 1) {
    difference |= (leftHash.charCodeAt(index) || 0) ^ (rightHash.charCodeAt(index) || 0);
  }
  return difference === 0;
}

async function sha256Hex(value) {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function responseHeaders() {
  return {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
  };
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: responseHeaders() });
}
