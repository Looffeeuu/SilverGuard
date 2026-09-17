const http = require("node:http");
const crypto = require("node:crypto");

const ZHIPU_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
const MODEL_NAME = "glm-4.7-flash";
const CLIENT_TOKEN_HEADER = "x-silverguard-client-token";
const MAX_REQUEST_BYTES = 24_000;
const MAX_TOTAL_MESSAGE_CHARS = 12_000;
const MAX_RESPONSE_CHARS = 80_000;

function runtimeConfig(env = process.env) {
  const parsedTimeout = Number(env.SILVERGUARD_UPSTREAM_TIMEOUT_MS || 2_500);
  return {
    zhipuApiKey: env.ZHIPU_API_KEY || "",
    appToken: env.SILVERGUARD_APP_TOKEN || "",
    upstreamTimeoutMs: Number.isFinite(parsedTimeout)
      ? Math.min(Math.max(parsedTimeout, 500), 50_000)
      : 2_500,
  };
}

function responseHeaders(extra = {}) {
  return {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    ...extra,
  };
}

function sendJson(response, status, body, extraHeaders) {
  response.writeHead(status, responseHeaders(extraHeaders));
  response.end(JSON.stringify(body));
}

function readRequestBody(request) {
  return new Promise((resolve, reject) => {
    const declaredLength = Number(request.headers["content-length"] || 0);
    if (declaredLength > MAX_REQUEST_BYTES) {
      reject(new RequestError(413, "request_too_large"));
      request.resume();
      return;
    }

    let size = 0;
    const chunks = [];
    request.on("data", (chunk) => {
      size += chunk.length;
      if (size > MAX_REQUEST_BYTES) {
        reject(new RequestError(413, "request_too_large"));
        request.destroy();
        return;
      }
      chunks.push(chunk);
    });
    request.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    request.on("error", reject);
  });
}

function validateMessages(messages) {
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
  return clean.some((message) => message.role === "user") ? clean : null;
}

function secureTokenEquals(suppliedToken, expectedToken) {
  if (!suppliedToken || !expectedToken) return false;
  const suppliedHash = crypto.createHash("sha256").update(suppliedToken).digest();
  const expectedHash = crypto.createHash("sha256").update(expectedToken).digest();
  return crypto.timingSafeEqual(suppliedHash, expectedHash);
}

async function handleChatRequest(request, config, fetchImpl = fetch) {
  if (!config.zhipuApiKey || !config.appToken) {
    return { status: 503, body: { error: "proxy_not_configured" } };
  }

  const suppliedToken = String(request.headers[CLIENT_TOKEN_HEADER] || "");
  if (!secureTokenEquals(suppliedToken, config.appToken)) {
    return { status: 401, body: { error: "unauthorized" } };
  }

  let incoming;
  try {
    incoming = JSON.parse(await readRequestBody(request));
  } catch (error) {
    if (error instanceof RequestError) return { status: error.status, body: { error: error.code } };
    return { status: 400, body: { error: "invalid_json" } };
  }

  const messages = validateMessages(incoming?.messages);
  if (!messages) return { status: 400, body: { error: "invalid_messages" } };

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), config.upstreamTimeoutMs);
  let upstream;
  try {
    upstream = await fetchImpl(ZHIPU_ENDPOINT, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${config.zhipuApiKey}`,
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        model: MODEL_NAME,
        messages,
        temperature: 0.2,
        max_tokens: 900,
        thinking: { type: "disabled" },
        stream: false,
        response_format: { type: "json_object" },
      }),
      signal: controller.signal,
    });
    // The deadline covers the response body too, not just receipt of its headers.
    const responseText = await upstream.text();
    if (responseText.length > MAX_RESPONSE_CHARS) {
      return { status: 502, body: { error: "upstream_response_too_large" } };
    }
    if (!upstream.ok) {
      return {
        status: upstream.status === 429 ? 429 : upstream.status >= 500 ? 503 : 502,
        body: { error: "upstream_error" },
      };
    }
    return { status: 200, rawBody: responseText };
  } catch {
    return { status: 503, body: { error: controller.signal.aborted ? "upstream_timeout" : "upstream_unavailable" } };
  } finally {
    clearTimeout(timeout);
  }
}

function createServer(config = runtimeConfig(), fetchImpl = fetch) {
  return http.createServer(async (request, response) => {
    const pathname = new URL(request.url, "http://127.0.0.1").pathname.replace(/\/+$/, "") || "/";
    // CloudBase deployments may retain a gateway prefix such as /api/function-name.
    // Match the final route segment so the same server works through either routing mode.
    const isHealthPath = pathname === "/health" || pathname.endsWith("/health");
    const isChatPath = pathname === "/chat" || pathname.endsWith("/chat");
    if (request.method === "GET" && isHealthPath) {
      sendJson(response, 200, {
        status: "ok",
        model: MODEL_NAME,
        service: "cloudbase",
        configured: Boolean(config.zhipuApiKey && config.appToken),
      });
      return;
    }
    if (!isChatPath) {
      sendJson(response, 404, { error: "not_found" });
      return;
    }
    if (request.method !== "POST") {
      sendJson(response, 405, { error: "method_not_allowed" }, { Allow: "POST" });
      return;
    }

    try {
      const result = await handleChatRequest(request, config, fetchImpl);
      if (result.rawBody) {
        response.writeHead(result.status, responseHeaders());
        response.end(result.rawBody);
      } else {
        sendJson(response, result.status, result.body);
      }
    } catch {
      sendJson(response, 500, { error: "internal_error" });
    }
  });
}

class RequestError extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

if (require.main === module) {
  createServer().listen(9000, "0.0.0.0", () => {
    console.log("SilverGuard CloudBase proxy listening on port 9000");
  });
}

module.exports = {
  CLIENT_TOKEN_HEADER,
  MODEL_NAME,
  createServer,
  handleChatRequest,
  runtimeConfig,
  secureTokenEquals,
  validateMessages,
};
