import { handleChatRequest } from "../shared/proxy-core.js";

export async function onRequest({ request, env }) {
  if (request.method !== "POST") {
    return new Response(JSON.stringify({ error: "method_not_allowed" }), {
      status: 405,
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": "no-store",
        "Allow": "POST",
      },
    });
  }
  return handleChatRequest(request, env);
}
