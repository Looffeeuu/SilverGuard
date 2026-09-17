import assert from "node:assert/strict";
import test from "node:test";
import worker, { validateMessages } from "../src/index.js";

const validMessages = [
  { role: "system", content: "只返回 JSON" },
  { role: "user", content: "分析这段商品宣传" },
];

test("message validation rejects arbitrary roles and oversized input", () => {
  assert.equal(validateMessages([{ role: "assistant", content: "bad" }]), null);
  assert.equal(validateMessages([{ role: "user", content: "x".repeat(12_001) }]), null);
  assert.deepEqual(validateMessages(validMessages), validMessages);
});

test("proxy requires its own client token", async () => {
  const response = await worker.fetch(
    requestFor(validMessages),
    environment({ appToken: "expected-token" }),
  );
  assert.equal(response.status, 401);
});

test("proxy fixes model and adds upstream API authorization", async () => {
  const originalFetch = globalThis.fetch;
  let capturedUrl;
  let capturedOptions;
  globalThis.fetch = async (url, options) => {
    capturedUrl = url;
    capturedOptions = options;
    return new Response(JSON.stringify({ choices: [] }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  };

  try {
    const response = await worker.fetch(
      requestFor(validMessages, "expected-token", { model: "untrusted-model" }),
      environment({ appToken: "expected-token", apiKey: "server-only-api-key" }),
    );

    assert.equal(response.status, 200);
    assert.equal(capturedUrl, "https://open.bigmodel.cn/api/paas/v4/chat/completions");
    assert.equal(capturedOptions.headers.Authorization, "Bearer server-only-api-key");
    const body = JSON.parse(capturedOptions.body);
    assert.equal(body.model, "glm-4.7-flash");
    assert.deepEqual(body.response_format, { type: "json_object" });
    assert.equal(body.stream, false);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("proxy maps an upstream limit to a safe error", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("quota details", { status: 429 });
  try {
    const response = await worker.fetch(
      requestFor(validMessages, "expected-token"),
      environment({ appToken: "expected-token" }),
    );
    assert.equal(response.status, 429);
    assert.deepEqual(await response.json(), { error: "upstream_error" });
  } finally {
    globalThis.fetch = originalFetch;
  }
});

function requestFor(messages, token = "wrong-token", overrides = {}) {
  return new Request("https://proxy.example/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-SilverGuard-Client-Token": token,
    },
    body: JSON.stringify({ messages, ...overrides }),
  });
}

function environment({ appToken = "expected-token", apiKey = "zhipu-test-key" } = {}) {
  return {
    ZHIPU_API_KEY: apiKey,
    SILVERGUARD_APP_TOKEN: appToken,
    AI_RATE_LIMITER: { limit: async () => ({ success: true }) },
  };
}
