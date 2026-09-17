import test from "node:test";
import assert from "node:assert/strict";
import {
  handleChatRequest,
  handleHealthRequest,
  validateMessages,
} from "../edgeone/shared/proxy-core.js";

const ENV = {
  ZHIPU_API_KEY: "server-secret",
  SILVERGUARD_APP_TOKEN: "client-token",
};

test("EdgeOne health endpoint identifies the service", async () => {
  const response = handleHealthRequest();
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    status: "ok",
    model: "glm-4.7-flash",
    service: "edgeone",
    configured: false,
  });
});

test("EdgeOne health endpoint reports configured without exposing secrets", async () => {
  const response = handleHealthRequest(ENV);
  const payload = await response.json();
  assert.equal(payload.configured, true);
  assert.equal(JSON.stringify(payload).includes("server-secret"), false);
  assert.equal(JSON.stringify(payload).includes("client-token"), false);
});

test("EdgeOne proxy rejects a missing client token", async () => {
  const response = await handleChatRequest(
    new Request("https://example.com/v1/chat/completions", {
      method: "POST",
      body: JSON.stringify({ messages: [{ role: "user", content: "测试" }] }),
    }),
    ENV,
  );
  assert.equal(response.status, 401);
  assert.deepEqual(await response.json(), { error: "unauthorized" });
});

test("EdgeOne proxy forwards only the fixed safe model settings", async (t) => {
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });

  let forwarded;
  globalThis.fetch = async (_url, init) => {
    forwarded = JSON.parse(init.body);
    return new Response(JSON.stringify({ choices: [] }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  };

  const response = await handleChatRequest(
    new Request("https://example.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-SilverGuard-Client-Token": "client-token",
      },
      body: JSON.stringify({
        model: "attacker-controlled-model",
        messages: [{ role: "user", content: "测试商品宣传" }],
      }),
    }),
    ENV,
  );

  assert.equal(response.status, 200);
  assert.equal(forwarded.model, "glm-4.7-flash");
  assert.equal(forwarded.temperature, 0.2);
  assert.equal(forwarded.stream, false);
  assert.deepEqual(forwarded.response_format, { type: "json_object" });
  assert.deepEqual(forwarded.messages, [{ role: "user", content: "测试商品宣传" }]);
});

test("EdgeOne message validation rejects unsupported roles", () => {
  assert.equal(validateMessages([{ role: "assistant", content: "bad" }]), null);
});
