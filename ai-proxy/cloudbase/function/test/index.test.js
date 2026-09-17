const assert = require("node:assert/strict");
const http = require("node:http");
const test = require("node:test");
const { Readable } = require("node:stream");
const { createServer, handleChatRequest, validateMessages } = require("../index.js");

const CONFIG = {
  zhipuApiKey: "server-secret",
  appToken: "client-token",
  upstreamTimeoutMs: 2_500,
};

function request(server, path, options = {}) {
  return new Promise((resolve, reject) => {
    const address = server.address();
    const client = http.request({
      hostname: "127.0.0.1",
      port: address.port,
      path,
      method: options.method || "GET",
      headers: options.headers,
    }, (response) => {
      let body = "";
      response.on("data", (chunk) => { body += chunk; });
      response.on("end", () => resolve({ status: response.statusCode, body }));
    });
    client.on("error", reject);
    if (options.body) client.write(options.body);
    client.end();
  });
}

test("health never exposes configured secrets", async (t) => {
  const server = createServer(CONFIG);
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());

  const response = await request(server, "/health");
  assert.equal(response.status, 200);
  assert.match(response.body, /"configured":true/);
  assert.doesNotMatch(response.body, /server-secret|client-token/);
});

test("CloudBase gateway-prefixed health route is supported", async (t) => {
  const server = createServer(CONFIG);
  await new Promise((resolve) => server.listen(0, resolve));
  t.after(() => server.close());

  const response = await request(server, "/api/silverguard-ai-proxy/health");
  assert.equal(response.status, 200);
});

test("chat requires the application token", async (t) => {
  const server = createServer(CONFIG);
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());

  const response = await request(server, "/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ messages: [{ role: "user", content: "测试" }] }),
  });
  assert.equal(response.status, 401);
  assert.deepEqual(JSON.parse(response.body), { error: "unauthorized" });
});

test("chat always forwards the fixed model and safe request options", async (t) => {
  let upstreamBody;
  const fakeFetch = async (_url, options) => {
    upstreamBody = JSON.parse(options.body);
    return new Response(JSON.stringify({ choices: [{ message: { content: "{}" } }] }), { status: 200 });
  };
  const server = createServer(CONFIG, fakeFetch);
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => server.close());

  const response = await request(server, "/chat", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-SilverGuard-Client-Token": "client-token",
    },
    body: JSON.stringify({
      model: "untrusted-model",
      messages: [{ role: "user", content: "测试商品宣传" }],
    }),
  });

  assert.equal(response.status, 200);
  assert.equal(upstreamBody.model, "glm-4.7-flash");
  assert.equal(upstreamBody.temperature, 0.2);
  assert.equal(upstreamBody.max_tokens, 900);
  assert.equal(upstreamBody.stream, false);
  assert.deepEqual(upstreamBody.thinking, { type: "disabled" });
  assert.deepEqual(upstreamBody.response_format, { type: "json_object" });
});

test("message validation rejects unsupported roles", () => {
  assert.equal(validateMessages([{ role: "assistant", content: "不允许" }]), null);
});

test("deadline still applies while reading a slow upstream response body", async () => {
  const incoming = Readable.from([Buffer.from(JSON.stringify({
    messages: [{ role: "user", content: "test product" }],
  }))]);
  incoming.headers = { "x-silverguard-client-token": CONFIG.appToken };
  const fakeFetch = async (_url, { signal }) => ({
    ok: true,
    text: () => new Promise((resolve, reject) => {
      const fallback = setTimeout(() => resolve("{}"), 200);
      signal.addEventListener("abort", () => {
        clearTimeout(fallback);
        reject(new Error("body aborted"));
      }, { once: true });
    }),
  });
  const result = await handleChatRequest(incoming, { ...CONFIG, upstreamTimeoutMs: 20 }, fakeFetch);
  assert.equal(result.status, 503);
  assert.deepEqual(result.body, { error: "upstream_timeout" });
});
