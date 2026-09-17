import { handleHealthRequest } from "../shared/proxy-core.js";

export function onRequestGet({ env }) {
  return handleHealthRequest(env);
}
