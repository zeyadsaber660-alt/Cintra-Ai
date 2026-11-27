/**
 * Cintra AI — Secure Cloudflare Worker Proxy & HyperOS Frosted Liquid Glass Control Center v3
 *
 * Endpoints:
 *  - GET  / or /dashboard       -> Luxury Frosted Liquid Glass Admin Dashboard
 *  - GET  /api/admin/config     -> Load masked config (X-Admin-Secret)
 *  - POST /api/admin/config     -> Save provider keys & search key to Cloudflare KV
 *  - POST /api/admin/test       -> Live API key & model connectivity test with latency readout
 *  - POST /v1/chat/stream       -> Secure SSE streaming proxy for Cintra Android App (Bearer APP_TOKEN)
 */

const CONFIG_KEY = "cintra_models_config_v2";

const SMART_UI_DIRECTIVE = [
  "You are Cintra, a warm, crystal-clear bilingual Arabic/English AI tutor and assistant.",
  "- TOKEN EFFICIENCY RULE: Keep internal thinking under 40 words. NEVER write HTML, CSS, SVG, or full drafts inside thinking — put all code and answers directly in the final output.",
  "- For simple greetings (اهلا، مرحبا، hello), reply warmly in 1-2 short sentences with zero thinking.",
  "- Write clean natural Arabic (never mix random French/broken English words). NEVER write HTML entities like &mathbb;N — ALWAYS use real Unicode symbols (ℕ, ℤ, ℚ, ℝ, ℂ, ∅, ∪, ∩, ⊆).",
  "- When files/PDFs are attached, ALWAYS pull real examples, numbers, and exercises directly from the attached files.",
  "- INTERACTIVE WIDGETS: When the user asks for a calculator (savings, GPA, bill split, unit converter), interactive chart/simulation, or mini quiz/game, emit a self-contained Arabic RTL HTML+Tailwind+JS interactive widget inside ```cintra-widget\n...\n``` (only when `illustrativeImages` is enabled).",
  "- For educational explanations: intuitive definition + centered math equations in $$...$$ using Unicode symbols (∪, ∩, ∈, ∉, ⊆, ∅, ℕ, ℤ, ℝ) + NEVER use <br> inside Markdown tables + optional compact ```cintra-svg``` diagram (Venn/flowchart) + > callout boxes for rules/answers + short practice question at the end.",
  "- PDF GENERATION (RICH & COMPLETE): The app has 4 built-in Tailwind+Cairo RTL templates (study_guide, report, invoice, cheat_sheet). When creating a study/midterm/report PDF, NEVER make a tiny superficial summary — write a rich, comprehensive, exam-ready document with: clear explanations of EVERY topic, multiple step-by-step solved numerical examples (مثال محلول), structured tables, common exam traps (أخطاء شائعة), and practice questions. Always use HTML <sup> for exponents (e.g. 2<sup>n</sup>, f<sup>-1</sup>, x<sup>2</sup> — NEVER write ^). Use <span class=\"math\"> for inline math inside sentences and <div class=\"math-block\"> ONLY for standalone centered equations. Emit inside:",
  "```cintra-pdf",
  "TITLE: عنوان المستند",
  "FILENAME: summary.pdf",
  "TEMPLATE: study_guide",
  "SUMMARY: وصف مختصر",
  "---HTML---",
  '<h2>1. العنوان</h2><p>الشرح المختصر والمنظم...</p><div class="box-green"><strong>قاعدة ذهبية:</strong> ...</div>',
  "```",
  "- For image generation: ```genui-image\nDetailed English visual prompt\n```.",
  "- In PDFs, NEVER put Arabic words inside .math blocks — keep Arabic outside .math so RTL/LTR never reverses.",
  "- At the end of informative replies, optionally append 2-3 short Arabic follow-up suggestions inside ```cintra-suggestions\n[\"اقتراح 1\", \"اقتراح 2\"]\n```.",
  "- For other downloadable files: ```cintra-file\nTITLE: ...\nFILENAME: ...\nMIME: ...\nSUMMARY: ...\n---CONTENT---\n...\n```."
].join("\n");

const DEFAULT_CONFIG = {
  searchApiKey: "",
  imageApi: { baseUrl: "https://nexora-llm.onrender.com/v1", modelId: "auto", apiKey: "" },
  models: {
    "cintra-pro": {
      label: "Cintra Pro",
      baseUrl: "https://api.openai.com/v1",
      modelId: "gpt-4o",
      apiKey: "",
      systemPrompt: "You are Cintra Pro — precise, thorough, and skilled in coding and analysis."
    },
    "cintra-think": {
      label: "Cintra Think",
      baseUrl: "https://api.deepseek.com/v1",
      modelId: "deepseek-reasoner",
      apiKey: "",
      systemPrompt: "You are Cintra Think — reason carefully step-by-step before answering."
    },
    "cintra-core": {
      label: "Cintra Core",
      baseUrl: "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
      modelId: "qwen-plus",
      apiKey: "",
      systemPrompt: "You are Cintra Core — fast, crisp, and clear for everyday tasks."
    },
    "cintra-vision": {
      label: "Cintra Vision",
      baseUrl: "https://generativelanguage.googleapis.com/v1beta/openai",
      modelId: "gemini-2.5-pro",
      apiKey: "",
      systemPrompt: "You are Cintra Vision — expert in visual understanding, images, and documents."
    }
  }
};

function corsHeaders(extra = {}) {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Admin-Secret",
    ...extra
  };
}

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: corsHeaders({ "Content-Type": "application/json; charset=utf-8" })
  });
}

async function loadConfig(env) {
  if (!env.CINTRA_CONFIG) return JSON.parse(JSON.stringify(DEFAULT_CONFIG));
  const raw = await env.CINTRA_CONFIG.get(CONFIG_KEY);
  if (!raw) return JSON.parse(JSON.stringify(DEFAULT_CONFIG));
  try {
    const parsed = JSON.parse(raw);
    return {
      searchApiKey: parsed.searchApiKey || "",
      imageApi: { ...DEFAULT_CONFIG.imageApi, ...(parsed.imageApi || {}) },
      models: { ...JSON.parse(JSON.stringify(DEFAULT_CONFIG.models)), ...(parsed.models || {}) }
    };
  } catch (_e) {
    return JSON.parse(JSON.stringify(DEFAULT_CONFIG));
  }
}

function maskKey(key) {
  if (!key || key.length < 8) return key ? "••••" : "";
  return key.slice(0, 4) + "••••••••" + key.slice(-4);
}

function verifyAdmin(request, env) {
  if (!env.ADMIN_SECRET) return true;
  return (request.headers.get("X-Admin-Secret") || "") === env.ADMIN_SECRET;
}

function verifyAppToken(request, env) {
  if (!env.APP_TOKEN) return true;
  const token = (request.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "").trim();
  return token === env.APP_TOKEN;
}

function needsAutoWebSearch(prompt, tool, autoEnabled) {
  if (tool === "web") return true;
  if (!autoEnabled || !prompt) return false;
  return /(دور|دورلي|ابحث|بحث|صح ولا لا|حقيقة|تأكد|تأكيد|مصدر|مصادر|معلومة|وشاح|جامعة|كلية|فرقة|الفرقة|طرق السفر|سفر|السفر|فيزا|تأشيرة|هجرة|سعر|اسعار|أسعار|اليوم|أحدث|أخبار|طقس|2025|2026|2027|search|verify|fact check|sources|travel|visa|latest|news|price|weather|today|current)/i.test(prompt);
}

async function runAutoWebSearch(query, tavilyApiKey) {
  const sources = [];
  try {
    if (tavilyApiKey) {
      const resp = await fetch("https://api.tavily.com/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ api_key: tavilyApiKey, query, max_results: 5, include_answer: true, search_depth: "basic" })
      });
      if (resp.ok) {
        const data = await resp.json();
        const snippets = (data.results || []).map((r, i) => {
          if (r.url) sources.push({ title: r.title || r.url, url: r.url });
          return `[${i + 1}] ${r.title} (${r.url}): ${r.content}`;
        }).join("\n");
        return { text: snippets || data.answer || "", sources };
      }
    }
    const ddg = await fetch(`https://api.duckduckgo.com/?q=${encodeURIComponent(query)}&format=json&no_html=1`);
    if (ddg.ok) {
      const d = await ddg.json();
      if (d.AbstractURL) sources.push({ title: d.Heading || "Source", url: d.AbstractURL });
      if (d.AbstractText) return { text: `${d.Heading || ""}: ${d.AbstractText} (${d.AbstractURL || ""})`, sources };
    }
  } catch (_e) {}
  return { text: "", sources };
}



async function fetchUrlContext(prompt) {
  const m = (prompt || "").match(/https?:\/\/[^\s<>")]+/i);
  if (!m) return "";
  const targetUrl = m[0];
  try {
    // YouTube video understanding: title, channel, description, and auto-captions if available
    if (/(youtube\.com\/watch|youtu\.be\/|youtube\.com\/shorts)/i.test(targetUrl)) {
      const [oembedResp, pageResp] = await Promise.all([
        fetch(`https://www.youtube.com/oembed?url=${encodeURIComponent(targetUrl)}&format=json`).catch(() => null),
        fetch(targetUrl, { headers: { "User-Agent": "Mozilla/5.0", "Accept-Language": "ar,en;q=0.9" } }).catch(() => null)
      ]);
      let info = "";
      if (oembedResp && oembedResp.ok) {
        const oe = await oembedResp.json();
        info += `YouTube Video: ${oe.title || ""} | Channel: ${oe.author_name || ""}\n`;
      }
      if (pageResp && pageResp.ok) {
        const html = await pageResp.text();
        const descMatch = html.match(/"shortDescription":"((?:[^"\\]|\\.)*)"/);
        if (descMatch) {
          const desc = descMatch[1].replace(/\\n/g, "\n").replace(/\\"/g, '"').slice(0, 2500);
          info += `Description:\n${desc}\n`;
        }
        const capMatch = html.match(/"captionTracks":\[\{"baseUrl":"([^"]+)"/);
        if (capMatch) {
          const capUrl = capMatch[1].replace(/\\u0026/g, "&");
          const capResp = await fetch(capUrl).catch(() => null);
          if (capResp && capResp.ok) {
            const xml = await capResp.text();
            const transcript = xml.replace(/<[^>]+>/g, " ").replace(/\s{2,}/g, " ").trim().slice(0, 5000);
            if (transcript) info += `Transcript:\n${transcript}\n`;
          }
        }
      }
      return info.trim();
    }

    const r = await fetch(targetUrl, { headers: { "User-Agent": "CintraAI/19.0" } });
    if (!r.ok) return "";
    const html = await r.text();
    const text = html
      .replace(/<script[\s\S]*?<\/script>/gi, " ")
      .replace(/<style[\s\S]*?<\/style>/gi, " ")
      .replace(/<[^>]+>/g, " ")
      .replace(/\s{2,}/g, " ")
      .trim()
      .slice(0, 6000);
    return text ? `URL Content (${targetUrl}):\n${text}` : "";
  } catch (_e) {
    return "";
  }
}

function needsRealWebPhotos(prompt) {
  if (!prompt || prompt.trim().length < 6) return false;
  if (/^(اهلا|أهلا|أهلاً|مرحبا|مرحباً|هاي|هلو|السلام|ازيك|إزيك|اي ده|إيه ده|hello|hi|hey)\b/i.test(prompt.trim())) return false;
  return /(سفر|السفر|سياحة|السياحة|معالم|أماكن|اماكن|مدينة|مدن|صور|صورة|ألمانيا|المانيا|تركيا|أمريكا|امريكا|نيويورك|باريس|لندن|دبي|مصر|القاهرة|اليابان|إيطاليا|ايطاليا|جبل|شاطئ|travel|visit|city|landmarks|places|photos|germany|turkey|usa|paris|dubai)/i.test(prompt);
}

async function fetchRealWebPhotos(query, tavilyApiKey) {
  if (!needsRealWebPhotos(query)) return [];
  const photos = [];
  try {
    if (tavilyApiKey) {
      const r = await fetch("https://api.tavily.com/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ api_key: tavilyApiKey, query, max_results: 4, include_images: true, include_image_descriptions: true })
      });
      if (r.ok) {
        const d = await r.json();
        for (const img of (d.images || []).slice(0, 5)) {
          const url = typeof img === "string" ? img : img.url;
          const title = typeof img === "string" ? "" : (img.description || "");
          if (url && url.startsWith("https://")) photos.push({ url, title, source: "web" });
        }
        if (photos.length > 0) return photos;
      }
    }
    // Free Wikimedia Commons real-photo search (works in Arabic & English, zero API key needed)
    const wikiUrl = `https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=${encodeURIComponent(query)}&gsrlimit=6&prop=pageimages&piprop=thumbnail&pithumbsize=640&format=json&origin=*`;
    const wr = await fetch(wikiUrl, { headers: { "User-Agent": "CintraAI/4.0" } });
    if (wr.ok) {
      const wd = await wr.json();
      const pages = Object.values(wd?.query?.pages || {});
      for (const p of pages) {
        if (p?.thumbnail?.source && p.thumbnail.source.startsWith("https://")) {
          photos.push({ url: p.thumbnail.source, title: p.title || "", source: "wikipedia" });
        }
        if (photos.length >= 4) break;
      }
    }
  } catch (_e) {}
  return photos;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/dashboard")) {
      return new Response(DASHBOARD_HTML, {
        headers: { "Content-Type": "text/html; charset=utf-8" }
      });
    }

    if (request.method === "GET" && url.pathname === "/api/admin/config") {
      if (!verifyAdmin(request, env)) return jsonResponse({ error: "كلمة مرور الإدارة غير صحيحة" }, 401);
      const cfg = await loadConfig(env);
      const maskedModels = {};
      for (const [k, v] of Object.entries(cfg.models)) {
        maskedModels[k] = { ...v, apiKey: "", apiKeyMasked: maskKey(v.apiKey), hasKey: Boolean(v.apiKey) };
      }
      return jsonResponse({
        models: maskedModels,
        hasSearchKey: Boolean(cfg.searchApiKey),
        searchKeyMasked: maskKey(cfg.searchApiKey),
        imageApi: { baseUrl: cfg.imageApi?.baseUrl || "", modelId: cfg.imageApi?.modelId || "", hasKey: Boolean(cfg.imageApi?.apiKey), apiKeyMasked: maskKey(cfg.imageApi?.apiKey) },
        kvBound: Boolean(env.CINTRA_CONFIG)
      });
    }

    if (request.method === "POST" && url.pathname === "/api/admin/config") {
      if (!verifyAdmin(request, env)) return jsonResponse({ error: "كلمة مرور الإدارة غير صحيحة" }, 401);
      if (!env.CINTRA_CONFIG) return jsonResponse({ error: "اربط قاعدة بيانات CINTRA_CONFIG KV أولاً" }, 500);
      const existing = await loadConfig(env);
      const incoming = await request.json();
      const updated = {
        searchApiKey: incoming.searchApiKey && incoming.searchApiKey.trim() !== ""
          ? incoming.searchApiKey.trim()
          : existing.searchApiKey,
        imageApi: {
          baseUrl: (incoming.imageApi?.baseUrl || existing.imageApi?.baseUrl || "").trim().replace(/\/+$/, ""),
          modelId: (incoming.imageApi?.modelId || existing.imageApi?.modelId || "auto").trim(),
          apiKey: incoming.imageApi?.apiKey && incoming.imageApi.apiKey.trim() !== "" ? incoming.imageApi.apiKey.trim() : (existing.imageApi?.apiKey || "")
        },
        models: { ...existing.models }
      };

      for (const [modelKey, payload] of Object.entries(incoming.models || {})) {
        if (!updated.models[modelKey]) continue;
        updated.models[modelKey] = {
          label: payload.label || updated.models[modelKey].label,
          baseUrl: (payload.baseUrl || updated.models[modelKey].baseUrl).trim().replace(/\/+$/, ""),
          modelId: (payload.modelId || updated.models[modelKey].modelId).trim(),
          apiKey: payload.apiKey && payload.apiKey.trim() !== ""
            ? payload.apiKey.trim()
            : updated.models[modelKey].apiKey,
          systemPrompt: payload.systemPrompt ?? updated.models[modelKey].systemPrompt
        };
      }

      await env.CINTRA_CONFIG.put(CONFIG_KEY, JSON.stringify(updated));
      return jsonResponse({ ok: true });
    }

    // Live Model Connectivity Test
    if (request.method === "POST" && url.pathname === "/api/admin/test") {
      if (!verifyAdmin(request, env)) return jsonResponse({ error: "كلمة مرور الإدارة غير صحيحة" }, 401);
      const { modelKey } = await request.json();
      const cfg = await loadConfig(env);
      const route = cfg.models[modelKey];
      if (!route || !route.apiKey) {
        return jsonResponse({ ok: false, error: "لم يتم حفظ مفتاح API لهذا النموذج بعد" }, 400);
      }
      const start = Date.now();
      try {
        const resp = await fetch(`${route.baseUrl.replace(/\/+$/, "")}/chat/completions`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${route.apiKey}`
          },
          body: JSON.stringify({
            model: route.modelId,
            messages: [{ role: "user", content: "Reply with OK only." }],
            max_tokens: 12,
            stream: false
          })
        });
        const latencyMs = Date.now() - start;
        if (!resp.ok) {
          const txt = await resp.text();
          return jsonResponse({ ok: false, status: resp.status, latencyMs, error: txt.slice(0, 260) }, 200);
        }
        return jsonResponse({ ok: true, status: resp.status, latencyMs });
      } catch (e) {
        return jsonResponse({ ok: false, error: e.message || "Network error" }, 200);
      }
    }


    // Custom Image Generation API proxy (uses user's configured Image API from Dashboard)
    if (request.method === "POST" && url.pathname === "/v1/images/generate") {
      if (!verifyAppToken(request, env)) return jsonResponse({ error: "Unauthorized" }, 401);
      const { prompt } = await request.json();
      const cfg = await loadConfig(env);
      const imgCfg = cfg.imageApi || {};
      const key = imgCfg.apiKey || cfg.models["cintra-pro"]?.apiKey || "";
      const base = (imgCfg.baseUrl || cfg.models["cintra-pro"]?.baseUrl || "").replace(/\/+$/, "");
      if (!base || !key) {
        return jsonResponse({ error: "Image API not configured" }, 400);
      }
      try {
        const resp = await fetch(`${base}/images/generations`, {
          method: "POST",
          headers: { "Content-Type": "application/json", "Authorization": `Bearer ${key}` },
          body: JSON.stringify({ model: imgCfg.modelId || "auto", prompt, n: 1, size: "1024x1024" })
        });
        const data = await resp.json();
        const first = data?.data?.[0] || {};
        if (first.url || first.b64_json) {
          return jsonResponse({ url: first.url || "", b64_json: first.b64_json || "" });
        }
        return jsonResponse({ error: data?.error?.message || "Image API returned empty data" }, 502);
      } catch (e) {
        return jsonResponse({ error: e.message || "Image generation failed" }, 502);
      }
    }

    // Secure SSE Streaming Proxy for Android App
    if (request.method === "POST" && url.pathname === "/v1/chat/stream") {
      if (!verifyAppToken(request, env)) return jsonResponse({ error: "Unauthorized" }, 401);

      const body = await request.json();
      const modelKey = body.model || "cintra-pro";
      const cfg = await loadConfig(env);
      const route = cfg.models[modelKey] || cfg.models["cintra-pro"];

      if (!route || !route.apiKey) {
        return jsonResponse({ error: `لم يتم ضبط مفتاح API للنموذج ${modelKey} في لوحة التحكم` }, 400);
      }

      const incomingMessages = Array.isArray(body.messages) ? body.messages : [];
      const rawUserMsg = [...incomingMessages].reverse().find((m) => m.role === "user")?.content;
      const lastUserPrompt = typeof body.rawPrompt === "string" && body.rawPrompt.trim()
        ? body.rawPrompt.trim()
        : (typeof rawUserMsg === "string" ? rawUserMsg : "");

      const withTimeout = (promise, ms, fallback) =>
        Promise.race([promise, new Promise((res) => setTimeout(() => res(fallback), ms))]);

      const shouldSearch = needsAutoWebSearch(lastUserPrompt, body.tool, body.autoWebSearch !== false);
      const [urlContext, searchRes, prefetchedPhotos] = await Promise.all([
        withTimeout(fetchUrlContext(lastUserPrompt), 2800, ""),
        shouldSearch ? withTimeout(runAutoWebSearch(lastUserPrompt, cfg.searchApiKey), 2800, { text: "", sources: [] }) : Promise.resolve({ text: "", sources: [] }),
        body.illustrativeImages !== false ? withTimeout(fetchRealWebPhotos(lastUserPrompt, cfg.searchApiKey), 2200, []) : Promise.resolve([])
      ]);

      let webContext = "";
      let webSources = Array.isArray(searchRes?.sources) ? searchRes.sources : [];
      if (urlContext) webContext += urlContext + "\n\n";
      if (searchRes?.text) webContext += searchRes.text;

      const fullSystem = [
        route.systemPrompt || "",
        body.customInstructions ? `\nUser Custom Instructions:\n${body.customInstructions}` : "",
        body.generativeUi !== false ? SMART_UI_DIRECTIVE : "",
        webContext ? `\nLive Web Search Context:\n${webContext}` : ""
      ].filter(Boolean).join("\n");

      const upstreamResp = await fetch(`${route.baseUrl.replace(/\/+$/, "")}/chat/completions`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${route.apiKey}`
        },
        body: JSON.stringify({
          model: route.modelId,
          messages: [{ role: "system", content: fullSystem }, ...incomingMessages],
          stream: true,
          ...(modelKey !== "cintra-think" && body.tool !== "thinking" ? { reasoning_effort: "low" } : {})
        })
      });

      if (!upstreamResp.ok) {
        const errText = await upstreamResp.text();
        return jsonResponse({ error: `خطأ من المزود (${upstreamResp.status}): ${errText.slice(0, 220)}` }, upstreamResp.status);
      }

      const webPhotos = prefetchedPhotos || [];

      if (upstreamResp.body) {
        const encoder = new TextEncoder();
        const stream = new ReadableStream({
          async start(controller) {
            if (webContext) {
              controller.enqueue(encoder.encode(`data: ${JSON.stringify({ cintra_status: "تم البحث في الويب والتحقق من المصادر..." })}\n\n`));
            }
            if (webSources.length > 0) {
              controller.enqueue(encoder.encode(`data: ${JSON.stringify({ cintra_sources: webSources })}\n\n`));
            }
            if (webPhotos.length > 0) {
              controller.enqueue(encoder.encode(`data: ${JSON.stringify({ cintra_photos: webPhotos })}\n\n`));
            }
            const reader = upstreamResp.body.getReader();
            try {
              while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                controller.enqueue(value);
              }
            } finally {
              controller.close();
            }
          }
        });
        return new Response(stream, {
          status: 200,
          headers: corsHeaders({
            "Content-Type": "text/event-stream; charset=utf-8",
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive"
          })
        });
      }

      return new Response(upstreamResp.body, {
        status: 200,
        headers: corsHeaders({
          "Content-Type": "text/event-stream; charset=utf-8",
          "Cache-Control": "no-cache, no-transform",
          "Connection": "keep-alive"
        })
      });
    }

    return jsonResponse({ error: "Not Found" }, 404);
  }
};

const DASHBOARD_HTML = `<!DOCTYPE html>
<html lang="ar" dir="rtl" data-theme="dark">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Cintra Control Center</title>
  <link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans+Arabic:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet" />
  <style>
    :root { --spring: cubic-bezier(0.22, 1, 0.36, 1); --jelly: cubic-bezier(0.34, 1.45, 0.64, 1); }
    [data-theme="dark"] {
      --bg: #08090C;
      --mesh: radial-gradient(650px circle at 85% 8%, rgba(56,189,248,0.12), transparent 58%), radial-gradient(550px circle at 10% 90%, rgba(148,163,184,0.10), transparent 60%);
      --glass: rgba(20, 23, 31, 0.78);
      --card: rgba(25, 29, 38, 0.82);
      --input: #11141C;
      --text: #F5F7FA;
      --muted: #98A1B2;
      --border: rgba(255,255,255,0.10);
      --rim: rgba(255,255,255,0.28);
      --accent: #38BDF8;
      --accent-soft: rgba(56,189,248,0.14);
      --ok: #34D399;
      --ok-soft: rgba(52,211,153,0.15);
      --err: #FB7185;
      --err-soft: rgba(251,113,133,0.15);
      --solid: #F5F7FA;
      --solid-t: #08090C;
    }
    [data-theme="light"] {
      --bg: #EFF1F5;
      --mesh: radial-gradient(650px circle at 85% 8%, rgba(2,132,199,0.10), transparent 58%), radial-gradient(550px circle at 10% 90%, rgba(100,116,139,0.12), transparent 60%);
      --glass: rgba(255, 255, 255, 0.82);
      --card: rgba(255, 255, 255, 0.90);
      --input: #F4F6F9;
      --text: #0F131A;
      --muted: #556072;
      --border: rgba(15,19,26,0.09);
      --rim: rgba(255,255,255,0.98);
      --accent: #0284C7;
      --accent-soft: rgba(2,132,199,0.12);
      --ok: #059669;
      --ok-soft: rgba(5,150,105,0.12);
      --err: #E11D48;
      --err-soft: rgba(225,29,72,0.12);
      --solid: #0F131A;
      --solid-t: #FFFFFF;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'IBM Plex Sans Arabic', system-ui, sans-serif;
      background: var(--bg); background-image: var(--mesh); background-attachment: fixed;
      color: var(--text); min-height: 100vh; padding: 20px 16px 60px;
    }
    .container { max-width: 980px; margin: 0 auto; display: flex; flex-direction: column; gap: 18px; }
    .glass {
      background: var(--glass); backdrop-filter: blur(30px) saturate(175%); -webkit-backdrop-filter: blur(30px) saturate(175%);
      border: 1px solid var(--border); border-top-color: var(--rim);
      border-radius: 26px; padding: 20px; box-shadow: 0 18px 42px rgba(0,0,0,0.22);
    }
    .topbar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
    .brand { display: flex; align-items: center; gap: 12px; }
    .brand-icon {
      width: 44px; height: 44px; border-radius: 14px; background: var(--solid); color: var(--solid-t);
      display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 20px;
    }
    .capsule {
      display: inline-flex; align-items: center; gap: 4px; padding: 4px; border-radius: 999px;
      background: var(--input); border: 1px solid var(--border);
    }
    .btn {
      border: none; border-radius: 999px; padding: 9px 18px; font-family: inherit; font-weight: 600;
      font-size: 13px; cursor: pointer; transition: transform 0.22s var(--jelly), opacity 0.2s ease;
    }
    .btn:active { transform: scale(0.95); }
    .btn-primary { background: var(--solid); color: var(--solid-t); }
    .btn-ghost { background: transparent; color: var(--text); }
    .btn-soft { background: var(--accent-soft); color: var(--accent); padding: 7px 14px; font-size: 12px; }
    .auth-grid { display: grid; grid-template-columns: 1fr 1fr auto; gap: 12px; align-items: end; }
    @media (max-width: 720px) { .auth-grid { grid-template-columns: 1fr; } }
    .models-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 16px; }
    @media (max-width: 480px) { .models-grid { grid-template-columns: 1fr; } }
    .model-card {
      background: var(--card); border: 1px solid var(--border); border-top-color: var(--rim);
      border-radius: 24px; padding: 18px; display: flex; flex-direction: column; gap: 12px;
      transition: transform 0.25s var(--spring);
    }
    .model-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; flex-wrap: wrap; }
    .badge { font-size: 11.5px; font-weight: 600; padding: 4px 10px; border-radius: 999px; }
    .badge-ok { background: var(--ok-soft); color: var(--ok); }
    .badge-warn { background: var(--err-soft); color: var(--err); }
    .field { display: flex; flex-direction: column; gap: 5px; }
    label { font-size: 12px; font-weight: 600; color: var(--muted); }
    input, select {
      width: 100%; border: 1px solid var(--border); background: var(--input); color: var(--text);
      border-radius: 14px; padding: 10px 13px; font-family: inherit; font-size: 13px; outline: none;
    }
    input[dir="ltr"] { font-family: 'JetBrains Mono', monospace; font-size: 12.5px; }
    input:focus, select:focus { border-color: var(--accent); }
    .preset-row { display: flex; gap: 6px; flex-wrap: wrap; }
    .preset-chip {
      border: 1px solid var(--border); background: var(--input); color: var(--muted);
      border-radius: 999px; padding: 4px 10px; font-size: 11px; font-weight: 600; cursor: pointer;
    }
    .preset-chip:hover { color: var(--text); border-color: var(--accent); }
    .test-result { font-size: 12px; font-weight: 600; min-height: 18px; }
    .toast-bar {
      position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%) translateY(20px);
      padding: 10px 22px; border-radius: 999px; background: var(--solid); color: var(--solid-t);
      font-size: 13px; font-weight: 600; opacity: 0; pointer-events: none; transition: all 0.28s var(--jelly);
      z-index: 50; box-shadow: 0 10px 30px rgba(0,0,0,0.35);
    }
    .toast-bar.show { opacity: 1; transform: translateX(-50%) translateY(0); }
  </style>
</head>
<body>
  <div class="container">
    <header class="glass topbar">
      <div class="brand">
        <div class="brand-icon">C</div>
        <div>
          <h1 style="font-size:19px;">Cintra Control Center</h1>
          <p style="font-size:12.5px;color:var(--muted);">إدارة مفاتيح النماذج واختبار الاتصال الحي بأمان على Cloudflare KV</p>
        </div>
      </div>
      <div class="capsule">
        <button class="btn btn-ghost" id="themeBtn" type="button">تبديل المظهر</button>
        <button class="btn btn-primary" id="saveBtn" type="button">حفظ جميع الإعدادات</button>
      </div>
    </header>

    <section class="glass">
      <div class="auth-grid">
        <div class="field">
          <label for="adminSecret">كلمة مرور الإدارة (ADMIN_SECRET)</label>
          <input id="adminSecret" type="password" placeholder="أدخل ADMIN_SECRET لفتح الإعدادات" />
        </div>
        <div class="field">
          <label for="searchKey">مفتاح البحث التلقائي في الويب (Tavily — اختياري)</label>
          <input id="searchKey" type="password" dir="ltr" placeholder="tvly-... (اتركه فارغاً لاستخدام Fallback)" />
        </div>
        <button class="btn btn-primary" id="loadBtn" type="button" style="height:42px;">فتح وتحميل</button>
      </div>
    </section>

    <section class="models-grid" id="modelsGrid"></section>
  </div>

  <div class="toast-bar" id="toastBar"></div>

  <script>
    const PRESETS = [
      { name: 'Nexora Fast (أسرع)', url: 'https://nexora-llm.onrender.com/v1', model: 'auto:fast' },
      { name: 'Nexora Auto', url: 'https://nexora-llm.onrender.com/v1', model: 'auto' },
      { name: 'OpenAI', url: 'https://api.openai.com/v1', model: 'gpt-4o' },
      { name: 'DeepSeek', url: 'https://api.deepseek.com/v1', model: 'deepseek-reasoner' },
      { name: 'Qwen', url: 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus' },
      { name: 'Gemini', url: 'https://generativelanguage.googleapis.com/v1beta/openai', model: 'gemini-2.5-pro' },
      { name: 'OpenRouter', url: 'https://openrouter.ai/api/v1', model: 'deepseek/deepseek-r1' },
      { name: 'Groq', url: 'https://api.groq.com/openai/v1', model: 'llama-3.3-70b-versatile' }
    ];

    const grid = document.getElementById('modelsGrid');
    const secretEl = document.getElementById('adminSecret');
    const searchEl = document.getElementById('searchKey');
    const toastBar = document.getElementById('toastBar');
    let models = {};
    let toastTimer = null;

    function notify(msg) {
      toastBar.textContent = msg;
      toastBar.classList.add('show');
      clearTimeout(toastTimer);
      toastTimer = setTimeout(() => toastBar.classList.remove('show'), 2600);
    }

    document.getElementById('themeBtn').onclick = () => {
      const r = document.documentElement;
      r.setAttribute('data-theme', r.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
    };

    function render() {
      grid.innerHTML = '';
      for (const [key, m] of Object.entries(models)) {
        const card = document.createElement('div');
        card.className = 'model-card';
        var presetsHtml = PRESETS.map(function (p, i) {
          return '<button class="preset-chip" type="button" data-preset="' + key + ':' + i + '">' + p.name + '</button>';
        }).join('');
        card.innerHTML =
          '<div class="model-head">' +
            '<div>' +
              '<strong style="font-size:16px;">' + m.label + '</strong>' +
              '<span style="font-size:12px;color:var(--muted);margin-right:6px;">' + key + '</span>' +
            '</div>' +
            '<div style="display:flex;align-items:center;gap:8px;">' +
              '<span class="badge ' + (m.hasKey ? 'badge-ok' : 'badge-warn') + '">' + (m.hasKey ? 'متصل • ' + m.apiKeyMasked : 'بدون مفتاح') + '</span>' +
              '<button class="btn btn-soft" type="button" data-test="' + key + '">اختبار API</button>' +
            '</div>' +
          '</div>' +
          '<div class="preset-row">' + presetsHtml + '</div>' +
          '<div class="field"><label>Base URL (OpenAI-Compatible)</label><input data-k="' + key + '" data-f="baseUrl" value="' + (m.baseUrl || '') + '" dir="ltr" /></div>' +
          '<div class="field"><label>Model ID</label><input data-k="' + key + '" data-f="modelId" value="' + (m.modelId || '') + '" dir="ltr" /></div>' +
          '<div class="field"><label>API Key (اتركه فارغاً للإبقاء على المفتاح المحفوظ)</label><input data-k="' + key + '" data-f="apiKey" type="password" placeholder="sk-..." dir="ltr" /></div>' +
          '<div class="test-result" id="res-' + key + '"></div>';
        grid.appendChild(card);
      }
    }

    grid.addEventListener('click', async (e) => {
      const presetBtn = e.target.closest('[data-preset]');
      if (presetBtn) {
        const [key, idx] = presetBtn.getAttribute('data-preset').split(':');
        const p = PRESETS[Number(idx)];
        document.querySelector('input[data-k="' + key + '"][data-f="baseUrl"]').value = p.url;
        document.querySelector('input[data-k="' + key + '"][data-f="modelId"]').value = p.model;
        notify('تم تعبئة إعدادات ' + p.name);
        return;
      }

      const testBtn = e.target.closest('[data-test]');
      if (testBtn) {
        const key = testBtn.getAttribute('data-test');
        const out = document.getElementById('res-' + key);
        out.style.color = 'var(--accent)';
        out.textContent = 'جارٍ اختبار الاتصال بالـ API...';
        const res = await fetch('/api/admin/test', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Admin-Secret': secretEl.value },
          body: JSON.stringify({ modelKey: key })
        });
        const data = await res.json();
        if (data.ok) {
          out.style.color = 'var(--ok)';
          out.textContent = '✓ الاتصال ناجح (' + data.latencyMs + 'ms) — النموذج جاهز للتطبيق';
        } else {
          out.style.color = 'var(--err)';
          out.textContent = '✗ فشل الاختبار: ' + (data.error || ('HTTP ' + data.status));
        }
      }
    });

    async function loadConfig() {
      const res = await fetch('/api/admin/config', { headers: { 'X-Admin-Secret': secretEl.value } });
      const data = await res.json();
      if (!res.ok) { notify(data.error || 'فشل التحميل'); return; }
      models = data.models;
      render();
      notify(data.kvBound ? 'تم تحميل الإعدادات بنجاح' : 'تنبيه: اربط CINTRA_CONFIG أولاً');
    }

    async function saveConfig() {
      const payload = {};
      document.querySelectorAll('input[data-k]').forEach((inp) => {
        const k = inp.getAttribute('data-k'), f = inp.getAttribute('data-f');
        if (!payload[k]) payload[k] = { ...models[k] };
        payload[k][f] = inp.value;
      });
      const res = await fetch('/api/admin/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Admin-Secret': secretEl.value },
        body: JSON.stringify({ models: payload, searchApiKey: searchEl.value })
      });
      const data = await res.json();
      if (res.ok) {
        searchEl.value = '';
        notify('✓ تم حفظ جميع مفاتيح الـ API بأمان في Cloudflare KV');
        loadConfig();
      } else {
        notify(data.error || 'فشل الحفظ');
      }
    }

    document.getElementById('loadBtn').onclick = loadConfig;
    document.getElementById('saveBtn').onclick = saveConfig;
    loadConfig().catch(() => {});
  </script>
</body>
</html>`;
