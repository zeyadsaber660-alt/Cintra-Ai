/**
 * Cintra AI — Secure Cloudflare Worker Proxy & Multi-Page Frosted Liquid Glass Control Center v20
 *
 * Endpoints:
 *  - GET  / or /dashboard       -> 5-Page Frosted Liquid Glass Admin Control Center (Persistent Login)
 *  - GET  /v1/app/status        -> Remote Maintenance Mode, Broadcast Announcement & Feature Flags
 *  - GET  /api/admin/config     -> Load full masked config + live stats (X-Admin-Secret)
 *  - POST /api/admin/config     -> Save models, image API, search key, maintenance & broadcast to KV
 *  - POST /api/admin/test       -> Live per-model API connectivity test with latency readout
 *  - POST /v1/images/generate   -> Custom Image Generation API proxy (Bearer APP_TOKEN)
 *  - POST /v1/chat/stream       -> Fast parallel-search SSE streaming proxy (Bearer APP_TOKEN)
 */

const CONFIG_KEY = "cintra_models_config_v2";
const STATS_KEY = "cintra_stats_v1";

const SMART_UI_DIRECTIVE = [
  "You are Cintra, a warm, crystal-clear bilingual Arabic/English AI tutor and assistant.",
  "- TOKEN EFFICIENCY: Keep internal thinking under 40 words. NEVER draft HTML, CSS, SVG, or full code inside thinking.",
  "- For simple greetings (اهلا، مرحبا، hello), reply warmly in 1-2 short sentences with zero thinking.",
  "- Write clean natural Arabic (never mix random French/broken English words). NEVER write &mathbb;N — ALWAYS use real Unicode symbols (ℕ, ℤ, ℚ, ℝ, ℂ, ∅, ∪, ∩, ⊆).",
  "- When PDFs/files are attached, ALWAYS pull real examples, numbers, and exercises directly from the attached files.",
  "- VISUAL & INTERACTIVE TOOLS (use only when genuinely helpful):",
  "  1. Flowcharts / Mindmaps / Sequence Diagrams -> emit valid Mermaid syntax inside ```mermaid\\n...\\n```.",
  "  2. Statistical Charts (bar, line, pie, doughnut) -> emit Chart.js config JSON inside ```cintra-chart\\n{\"type\":\"bar\",\"data\":{\"labels\":[\"A\",\"B\"],\"datasets\":[{\"label\":\"القيمة\",\"data\":[80,95]}]}}\\n```.",
  "  3. Interactive Practice Quiz (after study explanations) -> emit JSON inside ```cintra-quiz\\n{\"question\":\"سؤال تدريبي؟\",\"options\":[\"اختيار 1\",\"اختيار 2\",\"اختيار 3\",\"اختيار 4\"],\"answerIndex\":0,\"explanation\":\"سبب الإجابة الصحيحة\"}\\n```.",
  "  4. Interactive Calculators / Sliders / Mini-Apps -> emit self-contained Arabic RTL HTML+Tailwind+JS inside ```cintra-widget\\n...\\n```.",
  "  5. Venn Diagrams / Simple Geometry -> emit clean SVG with explicit viewBox inside ```cintra-svg\\n<svg viewBox=\"0 0 340 180\" ...>...</svg>\\n```.",
  "- Put standalone math equations on their own centered line wrapped in $$...$$ using ONLY clean Unicode symbols (NEVER write \\quad, \\;, \\{, \\} — write plain {1, 2, 3} and clean Arabic math terms like خاصية الإبدال / التجميع / التوزيع).",
  "- NEVER use <br> inside Markdown tables. Use > blockquotes for key rules/answers and ==word== for highlights.",
  "- PDF GENERATION (RICH & COMPLETE): Built-in Tailwind+Cairo RTL templates (study_guide, report, invoice, cheat_sheet). Write a rich exam-ready document with explanations, step-by-step solved numerical examples, tables, exam traps, and practice questions. Always use HTML <sup> for exponents (2<sup>n</sup>, f<sup>-1</sup>), <span class=\"math\"> inline, and <div class=\"math-block\"> for standalone equations. Emit inside:",
  "```cintra-pdf",
  "TITLE: عنوان المستند",
  "FILENAME: summary.pdf",
  "TEMPLATE: study_guide",
  "SUMMARY: وصف مختصر",
  "---HTML---",
  '<h2>1. العنوان</h2><p>الشرح المفصل والأمثلة...</p><div class="box-green"><strong>قاعدة ذهبية:</strong> ...</div>',
  "```",
  "- For image generation: ```genui-image\\nDetailed English visual prompt\\n```.",
  "- For other downloadable files (txt, json, csv, html, py): ```cintra-file\\nTITLE: ...\\nFILENAME: ...\\nMIME: ...\\nSUMMARY: ...\\n---CONTENT---\\n...\\n```.",
  "- Optionally end informative replies with 2-3 short Arabic follow-up chips inside ```cintra-suggestions\\n[\"اقتراح 1\", \"اقتراح 2\"]\\n```."
].join("\n");

const DEFAULT_CONFIG = {
  searchApiUrl: "https://api.tavily.com/search",
  searchApiKey: "",
  imageApi: { baseUrl: "https://nexora-llm.onrender.com/v1", modelId: "auto", apiKey: "" },
  maintenance: { enabled: false, message: "التطبيق قيد الصيانة السريعة حالياً لتحسين الأداء، سنعود خلال دقائق." },
  announcement: { enabled: false, title: "تحديث جديد", message: "" },
  features: { webSearch: true, visuals: true, pdfExport: true },
  models: {
    "cintra-pro": {
      label: "Cintra Pro",
      baseUrl: "https://nexora-llm.onrender.com/v1",
      modelId: "auto:fast",
      apiKey: "",
      systemPrompt: "You are Cintra Pro — precise, thorough, and skilled in coding and analysis."
    },
    "cintra-think": {
      label: "Cintra Think",
      baseUrl: "https://nexora-llm.onrender.com/v1",
      modelId: "auto",
      apiKey: "",
      systemPrompt: "You are Cintra Think — reason carefully step-by-step before answering."
    },
    "cintra-core": {
      label: "Cintra Core",
      baseUrl: "https://nexora-llm.onrender.com/v1",
      modelId: "auto:fast",
      apiKey: "",
      systemPrompt: "You are Cintra Core — fast, crisp, and clear for everyday tasks."
    },
    "cintra-vision": {
      label: "Cintra Vision",
      baseUrl: "https://nexora-llm.onrender.com/v1",
      modelId: "auto",
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
  const base = JSON.parse(JSON.stringify(DEFAULT_CONFIG));
  if (!env.CINTRA_CONFIG) return base;
  const raw = await env.CINTRA_CONFIG.get(CONFIG_KEY);
  if (!raw) return base;
  try {
    const parsed = JSON.parse(raw);
    return {
      searchApiUrl: parsed.searchApiUrl || "https://api.tavily.com/search",
      searchApiKey: parsed.searchApiKey || "",
      imageApi: { ...base.imageApi, ...(parsed.imageApi || {}) },
      maintenance: { ...base.maintenance, ...(parsed.maintenance || {}) },
      announcement: { ...base.announcement, ...(parsed.announcement || {}) },
      features: { ...base.features, ...(parsed.features || {}) },
      models: { ...base.models, ...(parsed.models || {}) }
    };
  } catch (_e) {
    return base;
  }
}

async function loadStats(env) {
  if (!env.CINTRA_CONFIG) return { totalRequests: 0, lastRequestAt: "" };
  try {
    const raw = await env.CINTRA_CONFIG.get(STATS_KEY);
    return raw ? JSON.parse(raw) : { totalRequests: 0, lastRequestAt: "" };
  } catch (_e) {
    return { totalRequests: 0, lastRequestAt: "" };
  }
}

async function bumpStats(env) {
  if (!env.CINTRA_CONFIG) return;
  try {
    const s = await loadStats(env);
    s.totalRequests = (s.totalRequests || 0) + 1;
    s.lastRequestAt = new Date().toISOString();
    await env.CINTRA_CONFIG.put(STATS_KEY, JSON.stringify(s));
  } catch (_e) {}
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
  return /(دور|دورلي|ابحث|بحث|صح ولا لا|حقيقة|تأكد|تأكيد|مصدر|مصادر|وشاح|طرق السفر|سفر|السفر|فيزا|تأشيرة|هجرة|سعر|اسعار|أسعار|اليوم|أحدث|أخبار|طقس|2025|2026|2027|search|verify|fact check|travel|visa|latest|news|price|weather|today|current)/i.test(prompt);
}

async function runAutoWebSearch(query, tavilyApiKey, searchApiUrl) {
  const sources = [];
  try {
    if (tavilyApiKey) {
      const resp = await fetch(searchApiUrl || "https://api.tavily.com/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ api_key: tavilyApiKey, query, max_results: 4, include_answer: true, search_depth: "basic" })
      });
      if (resp.ok) {
        const data = await resp.json();
        const snippets = (data.results || []).map((r, i) => {
          if (r.url) sources.push({ title: r.title || r.url, url: r.url });
          return "[" + (i + 1) + "] " + r.title + " (" + r.url + "): " + r.content;
        }).join("\n");
        return { text: snippets || data.answer || "", sources };
      }
    }
    const ddg = await fetch("https://api.duckduckgo.com/?q=" + encodeURIComponent(query) + "&format=json&no_html=1");
    if (ddg.ok) {
      const d = await ddg.json();
      if (d.AbstractURL) sources.push({ title: d.Heading || "Source", url: d.AbstractURL });
      if (d.AbstractText) return { text: (d.Heading || "") + ": " + d.AbstractText + " (" + (d.AbstractURL || "") + ")", sources };
    }
  } catch (_e) {}
  return { text: "", sources };
}

async function fetchUrlContext(prompt) {
  const m = (prompt || "").match(/https?:\/\/[^\s<>")]+/i);
  if (!m) return "";
  const targetUrl = m[0];
  try {
    if (/(youtube\.com\/watch|youtu\.be\/|youtube\.com\/shorts)/i.test(targetUrl)) {
      const [oembedResp, pageResp] = await Promise.all([
        fetch("https://www.youtube.com/oembed?url=" + encodeURIComponent(targetUrl) + "&format=json").catch(() => null),
        fetch(targetUrl, { headers: { "User-Agent": "Mozilla/5.0", "Accept-Language": "ar,en;q=0.9" } }).catch(() => null)
      ]);
      let info = "";
      if (oembedResp && oembedResp.ok) {
        const oe = await oembedResp.json();
        info += "YouTube Video: " + (oe.title || "") + " | Channel: " + (oe.author_name || "") + "\n";
      }
      if (pageResp && pageResp.ok) {
        const html = await pageResp.text();
        const descMatch = html.match(/"shortDescription":"((?:[^"\\]|\\.)*)"/);
        if (descMatch) {
          info += "Description:\n" + descMatch[1].replace(/\\n/g, "\n").replace(/\\"/g, '"').slice(0, 2500) + "\n";
        }
        const capMatch = html.match(/"captionTracks":\[\{"baseUrl":"([^"]+)"/);
        if (capMatch) {
          const capUrl = capMatch[1].replace(/\\u0026/g, "&");
          const capResp = await fetch(capUrl).catch(() => null);
          if (capResp && capResp.ok) {
            const xml = await capResp.text();
            const transcript = xml.replace(/<[^>]+>/g, " ").replace(/\s{2,}/g, " ").trim().slice(0, 5000);
            if (transcript) info += "Transcript:\n" + transcript + "\n";
          }
        }
      }
      return info.trim();
    }
    const r = await fetch(targetUrl, { headers: { "User-Agent": "CintraAI/20.0" } });
    if (!r.ok) return "";
    const html = await r.text();
    const text = html.replace(/<script[\s\S]*?<\/script>/gi, " ").replace(/<style[\s\S]*?<\/style>/gi, " ").replace(/<[^>]+>/g, " ").replace(/\s{2,}/g, " ").trim().slice(0, 6000);
    return text ? "URL Content (" + targetUrl + "):\n" + text : "";
  } catch (_e) {
    return "";
  }
}

function needsRealWebPhotos(prompt) {
  if (!prompt || prompt.trim().length < 6) return false;
  if (/^(اهلا|أهلا|أهلاً|مرحبا|مرحباً|هاي|هلو|السلام|ازيك|إزيك|اي ده|إيه ده|hello|hi|hey)\b/i.test(prompt.trim())) return false;
  return /(سفر|السفر|سياحة|السياحة|معالم|أماكن|اماكن|مدينة|مدن|صور|صورة|ألمانيا|المانيا|تركيا|أمريكا|امريكا|نيويورك|باريس|لندن|دبي|مصر|القاهرة|اليابان|إيطاليا|ايطاليا|جبل|شاطئ|travel|visit|city|landmarks|places|photos)/i.test(prompt);
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
          const u = typeof img === "string" ? img : img.url;
          const title = typeof img === "string" ? "" : (img.description || "");
          if (u && u.startsWith("https://")) photos.push({ url: u, title, source: "web" });
        }
        if (photos.length > 0) return photos;
      }
    }
  } catch (_e) {}
  return photos;
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/dashboard")) {
      return new Response(DASHBOARD_HTML, {
        headers: { "Content-Type": "text/html; charset=utf-8" }
      });
    }

    // Remote App Status (Maintenance Mode, Broadcast Banner, Feature Flags)
    if (request.method === "GET" && url.pathname === "/v1/app/status") {
      const cfg = await loadConfig(env);
      return jsonResponse({
        maintenance: cfg.maintenance,
        announcement: cfg.announcement,
        features: cfg.features
      });
    }

    if (request.method === "GET" && url.pathname === "/api/admin/config") {
      if (!verifyAdmin(request, env)) return jsonResponse({ error: "كلمة مرور الإدارة غير صحيحة" }, 401);
      const [cfg, stats] = await Promise.all([loadConfig(env), loadStats(env)]);
      const maskedModels = {};
      for (const [k, v] of Object.entries(cfg.models)) {
        maskedModels[k] = { ...v, apiKey: "", apiKeyMasked: maskKey(v.apiKey), hasKey: Boolean(v.apiKey) };
      }
      return jsonResponse({
        models: maskedModels,
        searchApiUrl: cfg.searchApiUrl || "https://api.tavily.com/search",
        hasSearchKey: Boolean(cfg.searchApiKey),
        searchKeyMasked: maskKey(cfg.searchApiKey),
        imageApi: {
          baseUrl: cfg.imageApi?.baseUrl || "",
          modelId: cfg.imageApi?.modelId || "",
          hasKey: Boolean(cfg.imageApi?.apiKey),
          apiKeyMasked: maskKey(cfg.imageApi?.apiKey)
        },
        maintenance: cfg.maintenance,
        announcement: cfg.announcement,
        features: cfg.features,
        stats,
        kvBound: Boolean(env.CINTRA_CONFIG)
      });
    }

    if (request.method === "POST" && url.pathname === "/api/admin/config") {
      if (!verifyAdmin(request, env)) return jsonResponse({ error: "كلمة مرور الإدارة غير صحيحة" }, 401);
      if (!env.CINTRA_CONFIG) return jsonResponse({ error: "اربط قاعدة بيانات CINTRA_CONFIG KV أولاً" }, 500);
      const existing = await loadConfig(env);
      const incoming = await request.json();
      const updated = {
        searchApiUrl: (incoming.searchApiUrl || existing.searchApiUrl || "https://api.tavily.com/search").trim(),
        searchApiKey: incoming.searchApiKey && incoming.searchApiKey.trim() !== ""
          ? incoming.searchApiKey.trim()
          : existing.searchApiKey,
        imageApi: {
          baseUrl: (incoming.imageApi?.baseUrl ?? existing.imageApi?.baseUrl ?? "").trim().replace(/\/+$/, ""),
          modelId: (incoming.imageApi?.modelId ?? existing.imageApi?.modelId ?? "auto").trim(),
          apiKey: incoming.imageApi?.apiKey && incoming.imageApi.apiKey.trim() !== ""
            ? incoming.imageApi.apiKey.trim()
            : (existing.imageApi?.apiKey || "")
        },
        maintenance: incoming.maintenance ? { ...existing.maintenance, ...incoming.maintenance } : existing.maintenance,
        announcement: incoming.announcement ? { ...existing.announcement, ...incoming.announcement } : existing.announcement,
        features: incoming.features ? { ...existing.features, ...incoming.features } : existing.features,
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
        const resp = await fetch(route.baseUrl.replace(/\/+$/, "") + "/chat/completions", {
          method: "POST",
          headers: { "Content-Type": "application/json", "Authorization": "Bearer " + route.apiKey },
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

    if (request.method === "POST" && url.pathname === "/v1/images/generate") {
      if (!verifyAppToken(request, env)) return jsonResponse({ error: "Unauthorized" }, 401);
      const { prompt } = await request.json();
      const cfg = await loadConfig(env);
      const imgCfg = cfg.imageApi || {};
      const key = imgCfg.apiKey || cfg.models["cintra-pro"]?.apiKey || "";
      const base = (imgCfg.baseUrl || cfg.models["cintra-pro"]?.baseUrl || "").replace(/\/+$/, "");
      if (!base || !key) return jsonResponse({ error: "Image API not configured" }, 400);
      try {
        const resp = await fetch(base + "/images/generations", {
          method: "POST",
          headers: { "Content-Type": "application/json", "Authorization": "Bearer " + key },
          body: JSON.stringify({ model: imgCfg.modelId || "auto", prompt, n: 1, size: "1024x1024" })
        });
        const data = await resp.json();
        const first = data?.data?.[0] || {};
        if (first.url || first.b64_json) return jsonResponse({ url: first.url || "", b64_json: first.b64_json || "" });
        return jsonResponse({ error: data?.error?.message || "Empty image response" }, 502);
      } catch (e) {
        return jsonResponse({ error: e.message || "Image generation failed" }, 502);
      }
    }

    if (request.method === "POST" && url.pathname === "/v1/chat/stream") {
      if (!verifyAppToken(request, env)) return jsonResponse({ error: "Unauthorized" }, 401);

      const cfg = await loadConfig(env);
      if (cfg.maintenance?.enabled) {
        return jsonResponse({ error: cfg.maintenance.message || "التطبيق في وضع الصيانة حالياً." }, 503);
      }
      if (ctx && typeof ctx.waitUntil === "function") ctx.waitUntil(bumpStats(env));

      const body = await request.json();
      const modelKey = body.model || "cintra-pro";
      const route = cfg.models[modelKey] || cfg.models["cintra-pro"];

      if (!route || !route.apiKey) {
        return jsonResponse({ error: "لم يتم ضبط مفتاح API للنموذج " + modelKey + " في لوحة التحكم" }, 400);
      }

      const incomingMessages = Array.isArray(body.messages) ? body.messages : [];
      const rawUserMsg = [...incomingMessages].reverse().find((m) => m.role === "user")?.content;
      const lastUserPrompt = typeof body.rawPrompt === "string" && body.rawPrompt.trim()
        ? body.rawPrompt.trim()
        : (typeof rawUserMsg === "string" ? rawUserMsg : "");

      const withTimeout = (promise, ms, fallback) =>
        Promise.race([promise, new Promise((res) => setTimeout(() => res(fallback), ms))]);

      const shouldSearch = cfg.features?.webSearch !== false && needsAutoWebSearch(lastUserPrompt, body.tool, body.autoWebSearch !== false);
      const [urlContext, searchRes, prefetchedPhotos] = await Promise.all([
        withTimeout(fetchUrlContext(lastUserPrompt), 2800, ""),
        shouldSearch ? withTimeout(runAutoWebSearch(lastUserPrompt, cfg.searchApiKey, cfg.searchApiUrl), 2800, { text: "", sources: [] }) : Promise.resolve({ text: "", sources: [] }),
        cfg.features?.visuals !== false && body.illustrativeImages !== false ? withTimeout(fetchRealWebPhotos(lastUserPrompt, cfg.searchApiKey), 2200, []) : Promise.resolve([])
      ]);

      let webContext = "";
      let webSources = Array.isArray(searchRes?.sources) ? searchRes.sources : [];
      if (urlContext) webContext += urlContext + "\n\n";
      if (searchRes?.text) webContext += searchRes.text;

      const fullSystem = [
        route.systemPrompt || "",
        body.customInstructions ? "\nUser Custom Instructions:\n" + body.customInstructions : "",
        body.generativeUi !== false ? SMART_UI_DIRECTIVE : "",
        webContext ? "\nLive Web & Video Context:\n" + webContext : ""
      ].filter(Boolean).join("\n");

      const upstreamResp = await fetch(route.baseUrl.replace(/\/+$/, "") + "/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + route.apiKey
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
        return jsonResponse({ error: "خطأ من المزود (" + upstreamResp.status + "): " + errText.slice(0, 220) }, upstreamResp.status);
      }

      const webPhotos = prefetchedPhotos || [];
      if (upstreamResp.body) {
        const encoder = new TextEncoder();
        const stream = new ReadableStream({
          async start(controller) {
            if (webContext) {
              controller.enqueue(encoder.encode("data: " + JSON.stringify({ cintra_status: "تم البحث والتحقق • جارٍ كتابة الإجابة..." }) + "\n\n"));
            }
            if (webSources.length > 0) {
              controller.enqueue(encoder.encode("data: " + JSON.stringify({ cintra_sources: webSources }) + "\n\n"));
            }
            if (webPhotos.length > 0) {
              controller.enqueue(encoder.encode("data: " + JSON.stringify({ cintra_photos: webPhotos }) + "\n\n"));
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
  <link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap" rel="stylesheet" />
  <style>
    :root { --bg:#0B0E14; --surface:rgba(20,25,36,0.82); --surface2:rgba(30,37,52,0.72); --text:#F1F5F9; --muted:#94A3B8; --border:rgba(255,255,255,0.10); --accent:#38BDF8; --ok:#34D399; --err:#FB7185; --warn:#FBBF24; }
    [data-theme="light"] { --bg:#F4F7FB; --surface:rgba(255,255,255,0.88); --surface2:#EEF2F7; --text:#0F172A; --muted:#64748B; --border:rgba(15,23,42,0.10); --accent:#0284C7; --ok:#059669; --err:#E11D48; --warn:#D97706; }
    *{box-sizing:border-box;font-family:'Cairo',sans-serif;}
    body{margin:0;background:var(--bg);color:var(--text);min-height:100vh;}
    .login-wrap{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;}
    .login-card{width:100%;max-width:400px;background:var(--surface);backdrop-filter:blur(24px);border:1px solid var(--border);border-radius:26px;padding:28px;box-shadow:0 24px 60px rgba(0,0,0,0.35);}
    .app{display:none;min-height:100vh;}
    .topbar{position:sticky;top:0;z-index:20;display:flex;align-items:center;justify-content:space-between;padding:14px 20px;background:var(--surface);backdrop-filter:blur(20px);border-bottom:1px solid var(--border);}
    .tabs{display:flex;gap:8px;overflow-x:auto;padding:12px 20px;border-bottom:1px solid var(--border);background:var(--surface);}
    .tab{padding:9px 16px;border-radius:999px;border:1px solid var(--border);background:transparent;color:var(--muted);font-weight:700;font-size:13px;cursor:pointer;white-space:nowrap;}
    .tab.active{background:var(--accent);color:#fff;border-color:var(--accent);}
    .container{max-width:980px;margin:0 auto;padding:20px;}
    .page{display:none;}
    .page.active{display:block;}
    .card{background:var(--surface);border:1px solid var(--border);border-radius:22px;padding:18px;margin-bottom:16px;}
    .grid2{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;}
    .stat-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px;margin-bottom:16px;}
    .stat{background:var(--surface2);border:1px solid var(--border);border-radius:18px;padding:16px;}
    .stat-v{font-size:24px;font-weight:800;margin-top:4px;color:var(--accent);}
    .field{margin-top:10px;}
    .field label{display:block;font-size:12px;color:var(--muted);margin-bottom:5px;font-weight:600;}
    input,textarea{width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--border);background:var(--surface2);color:var(--text);font-size:13.5px;outline:none;}
    .btn{padding:10px 18px;border-radius:12px;border:none;font-weight:700;font-size:13px;cursor:pointer;}
    .btn-primary{background:var(--accent);color:#fff;}
    .btn-soft{background:var(--surface2);color:var(--text);border:1px solid var(--border);}
    .btn-danger{background:rgba(251,113,133,0.16);color:var(--err);border:1px solid rgba(251,113,133,0.3);}
    .row{display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;}
    .chip{padding:5px 10px;border-radius:999px;font-size:11.5px;font-weight:700;background:var(--surface2);border:1px solid var(--border);color:var(--text);cursor:pointer;}
    .badge-ok{color:var(--ok);font-size:12px;font-weight:700;}
    .badge-warn{color:var(--warn);font-size:12px;font-weight:700;}
    .switch-row{display:flex;align-items:center;justify-content:space-between;padding:12px 0;border-bottom:1px solid var(--border);}
    .switch-row:last-child{border-bottom:none;}
    .toast{position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:#0F172A;color:#fff;padding:10px 20px;border-radius:999px;font-size:13px;font-weight:700;display:none;z-index:99;box-shadow:0 12px 30px rgba(0,0,0,0.4);}
  </style>
</head>
<body>
  <div class="login-wrap" id="loginView">
    <div class="login-card">
      <h2 style="margin:0 0 6px;">Cintra Control Center</h2>
      <p style="margin:0 0 18px;color:var(--muted);font-size:13px;">سجّل الدخول مرة واحدة فقط وسيتم حفظ جلستك تلقائياً</p>
      <div class="field">
        <label>كلمة مرور الإدارة (ADMIN_SECRET)</label>
        <input id="loginSecret" type="password" placeholder="أدخل ADMIN_SECRET..." dir="ltr" />
      </div>
      <button class="btn btn-primary" style="width:100%;margin-top:16px;" id="loginBtn">دخول للوحة التحكم</button>
    </div>
  </div>

  <div class="app" id="appView">
    <div class="topbar">
      <div class="row">
        <strong style="font-size:17px;">Cintra Control Center</strong>
        <span id="kvStatus" class="badge-ok"></span>
      </div>
      <div class="row">
        <button class="btn btn-primary" id="saveAllBtn">حفظ جميع التغييرات</button>
        <button class="btn btn-soft" id="themeBtn">المظهر</button>
        <button class="btn btn-danger" id="logoutBtn">تسجيل خروج</button>
      </div>
    </div>

    <div class="tabs">
      <button class="tab active" data-tab="models">1. النماذج والـ API</button>
      <button class="tab" data-tab="media">2. محرك الصور والبحث</button>
      <button class="tab" data-tab="control">3. الصيانة والتحكم</button>
      <button class="tab" data-tab="broadcast">4. الإشعارات العامة</button>
      <button class="tab" data-tab="stats">5. الإحصائيات والحالة</button>
    </div>

    <div class="container">
      <div class="page active" id="page-models">
        <div class="grid2" id="modelsGrid"></div>
      </div>

      <div class="page" id="page-media">
        <div class="card">
          <h3 style="margin:0 0 8px;">محرك توليد الصور الخاص بك (Image API)</h3>
          <p style="margin:0 0 12px;color:var(--muted);font-size:12.5px;">يربط ميزة توليد الصور داخل التطبيق مباشرة بـ API منصتك (/images/generations).</p>
          <div class="field"><label>Image Base URL</label><input id="imgBaseUrl" dir="ltr" placeholder="https://nexora-llm.onrender.com/v1" /></div>
          <div class="field"><label>Image Model ID</label><input id="imgModelId" dir="ltr" placeholder="auto" /></div>
          <div class="field"><label>Image API Key (اتركه فارغاً لاستخدام مفتاح Cintra Pro)</label><input id="imgApiKey" type="password" dir="ltr" placeholder="sk-..." /></div>
        </div>
        <div class="card">
          <h3 style="margin:0 0 8px;">محرك البحث المباشر (Tavily Search API)</h3>
          <div class="field"><label>Search API URL (افتراضي: Tavily أو رابط محركك)</label><input id="searchApiUrl" dir="ltr" placeholder="https://api.tavily.com/search" /></div>
          <div class="field"><label>Search API Key (اتركه فارغاً للإبقاء على المفتاح المحفوظ)</label><input id="searchApiKey" type="password" dir="ltr" placeholder="tvly-..." /></div>
          <div id="searchMasked" style="font-size:12px;color:var(--muted);margin-top:6px;"></div>
        </div>
      </div>

      <div class="page" id="page-control">
        <div class="card">
          <h3 style="margin:0 0 10px;">وضع الصيانة (إيقاف التطبيق مؤقتاً)</h3>
          <div class="switch-row">
            <div><strong>تفعيل وضع الصيانة</strong><div style="font-size:12px;color:var(--muted);">يمنع إرسال الطلبات ويظهر رسالة الصيانة للمستخدمين</div></div>
            <input type="checkbox" id="maintEnabled" style="width:22px;height:22px;" />
          </div>
          <div class="field"><label>رسالة الصيانة للمستخدمين</label><textarea id="maintMsg" rows="3"></textarea></div>
        </div>
        <div class="card">
          <h3 style="margin:0 0 10px;">التحكم في ميزات التطبيق عن بُعد</h3>
          <div class="switch-row"><span>تفعيل البحث التلقائي في الويب</span><input type="checkbox" id="featSearch" style="width:20px;height:20px;" /></div>
          <div class="switch-row"><span>تفعيل الصور والأدوات التفاعلية</span><input type="checkbox" id="featVisuals" style="width:20px;height:20px;" /></div>
          <div class="switch-row"><span>تفعيل توليد ملفات PDF</span><input type="checkbox" id="featPdf" style="width:20px;height:20px;" /></div>
        </div>
      </div>

      <div class="page" id="page-broadcast">
        <div class="card">
          <h3 style="margin:0 0 10px;">إشعار عام داخل التطبيق (Broadcast Banner)</h3>
          <div class="switch-row">
            <div><strong>إظهار الإشعار للمستخدمين عند فتح التطبيق</strong><div style="font-size:12px;color:var(--muted);">ممتاز للتنبيهات، التحديثات، أو العروض</div></div>
            <input type="checkbox" id="annEnabled" style="width:22px;height:22px;" />
          </div>
          <div class="field"><label>عنوان الإشعار</label><input id="annTitle" placeholder="تحديث جديد في Cintra" /></div>
          <div class="field"><label>نص الإشعار</label><textarea id="annMsg" rows="3" placeholder="أضفنا ميزة المخططات التفاعلية وتوليد PDF المحسّن!"></textarea></div>
        </div>
      </div>

      <div class="page" id="page-stats">
        <div class="stat-grid">
          <div class="stat"><div style="font-size:12px;color:var(--muted);">إجمالي الطلبات</div><div class="stat-v" id="statTotal">0</div></div>
          <div class="stat"><div style="font-size:12px;color:var(--muted);">النماذج المفعّلة</div><div class="stat-v" id="statModels">0 / 4</div></div>
          <div class="stat"><div style="font-size:12px;color:var(--muted);">حالة التطبيق</div><div class="stat-v" id="statStatus">يعمل</div></div>
        </div>
        <div class="card">
          <div style="font-size:13px;color:var(--muted);">آخر نشاط مسجّل: <strong id="statLast" style="color:var(--text);">—</strong></div>
        </div>
      </div>
    </div>
  </div>
  <div class="toast" id="toast"></div>

  <script>
    var STORAGE_KEY = 'cintra_admin_secret_v20';
    var PRESETS = [
      { name: 'Nexora Fast', url: 'https://nexora-llm.onrender.com/v1', model: 'auto:fast' },
      { name: 'Nexora Auto', url: 'https://nexora-llm.onrender.com/v1', model: 'auto' },
      { name: 'Gemini 2.5 Flash', url: 'https://generativelanguage.googleapis.com/v1beta/openai', model: 'gemini-2.5-flash' },
      { name: 'DeepSeek', url: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
      { name: 'OpenAI GPT-4o', url: 'https://api.openai.com/v1', model: 'gpt-4o' }
    ];
    var models = {};
    var adminSecret = localStorage.getItem(STORAGE_KEY) || '';

    function notify(msg) {
      var t = document.getElementById('toast');
      t.textContent = msg;
      t.style.display = 'block';
      clearTimeout(window._tt);
      window._tt = setTimeout(function(){ t.style.display = 'none'; }, 2800);
    }

    document.getElementById('themeBtn').onclick = function() {
      var r = document.documentElement;
      r.setAttribute('data-theme', r.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
    };

    document.querySelectorAll('.tab').forEach(function(btn) {
      btn.onclick = function() {
        document.querySelectorAll('.tab').forEach(function(b){ b.classList.remove('active'); });
        document.querySelectorAll('.page').forEach(function(p){ p.classList.remove('active'); });
        btn.classList.add('active');
        document.getElementById('page-' + btn.getAttribute('data-tab')).classList.add('active');
      };
    });

    function renderModels() {
      var grid = document.getElementById('modelsGrid');
      grid.innerHTML = '';
      var activeCount = 0;
      Object.keys(models).forEach(function(key) {
        var m = models[key];
        if (m.hasKey) activeCount++;
        var presetsHtml = PRESETS.map(function(p, i) {
          return '<button class="chip" type="button" data-preset="' + key + ':' + i + '">' + p.name + '</button>';
        }).join('');
        var card = document.createElement('div');
        card.className = 'card';
        card.innerHTML =
          '<div class="row"><div><strong>' + m.label + '</strong> <span style="font-size:12px;color:var(--muted);">' + key + '</span></div>' +
          '<div class="row"><span class="' + (m.hasKey ? 'badge-ok' : 'badge-warn') + '">' + (m.hasKey ? 'متصل • ' + m.apiKeyMasked : 'بدون مفتاح') + '</span>' +
          '<button class="btn btn-soft" style="padding:6px 10px;font-size:12px;" data-test="' + key + '">اختبار</button></div></div>' +
          '<div class="row" style="margin-top:10px;justify-content:flex-start;">' + presetsHtml + '</div>' +
          '<div class="field"><label>Base URL</label><input data-k="' + key + '" data-f="baseUrl" value="' + (m.baseUrl || '') + '" dir="ltr" /></div>' +
          '<div class="field"><label>Model ID</label><input data-k="' + key + '" data-f="modelId" value="' + (m.modelId || '') + '" dir="ltr" /></div>' +
          '<div class="field"><label>API Key (فارغ = الإبقاء على المحفوظ)</label><input data-k="' + key + '" data-f="apiKey" type="password" placeholder="sk-..." dir="ltr" /></div>' +
          '<div id="res-' + key + '" style="font-size:12px;margin-top:8px;"></div>';
        grid.appendChild(card);
      });
      document.getElementById('statModels').textContent = activeCount + ' / 4';
    }

    document.getElementById('modelsGrid').addEventListener('click', async function(e) {
      var pb = e.target.closest('[data-preset]');
      if (pb) {
        var parts = pb.getAttribute('data-preset').split(':');
        var p = PRESETS[Number(parts[1])];
        document.querySelector('input[data-k="' + parts[0] + '"][data-f="baseUrl"]').value = p.url;
        document.querySelector('input[data-k="' + parts[0] + '"][data-f="modelId"]').value = p.model;
        notify('تم تعبئة ' + p.name);
        return;
      }
      var tb = e.target.closest('[data-test]');
      if (tb) {
        var key = tb.getAttribute('data-test');
        var out = document.getElementById('res-' + key);
        out.style.color = 'var(--accent)';
        out.textContent = 'جارٍ الاختبار...';
        var r = await fetch('/api/admin/test', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Admin-Secret': adminSecret },
          body: JSON.stringify({ modelKey: key })
        });
        var d = await r.json();
        out.style.color = d.ok ? 'var(--ok)' : 'var(--err)';
        out.textContent = d.ok ? ('✓ ناجح (' + d.latencyMs + 'ms)') : ('✗ فشل: ' + (d.error || d.status));
      }
    });

    async function loadDashboard(secret) {
      var res = await fetch('/api/admin/config', { headers: { 'X-Admin-Secret': secret } });
      var data = await res.json();
      if (!res.ok) {
        localStorage.removeItem(STORAGE_KEY);
        notify(data.error || 'كلمة المرور غير صحيحة');
        return false;
      }
      adminSecret = secret;
      localStorage.setItem(STORAGE_KEY, secret);
      document.getElementById('loginView').style.display = 'none';
      document.getElementById('appView').style.display = 'block';

      models = data.models || {};
      renderModels();
      document.getElementById('kvStatus').textContent = data.kvBound ? 'KV متصل ✓' : 'اربط KV أولاً';
      document.getElementById('imgBaseUrl').value = (data.imageApi && data.imageApi.baseUrl) || '';
      document.getElementById('imgModelId').value = (data.imageApi && data.imageApi.modelId) || 'auto';
      document.getElementById('searchApiUrl').value = data.searchApiUrl || 'https://api.tavily.com/search';
      document.getElementById('searchMasked').textContent = data.hasSearchKey ? ('المفتاح المحفوظ: ' + data.searchKeyMasked) : 'لا يوجد مفتاح بحث محفوظ';
      document.getElementById('maintEnabled').checked = Boolean(data.maintenance && data.maintenance.enabled);
      document.getElementById('maintMsg').value = (data.maintenance && data.maintenance.message) || '';
      document.getElementById('annEnabled').checked = Boolean(data.announcement && data.announcement.enabled);
      document.getElementById('annTitle').value = (data.announcement && data.announcement.title) || '';
      document.getElementById('annMsg').value = (data.announcement && data.announcement.message) || '';
      document.getElementById('featSearch').checked = !(data.features && data.features.webSearch === false);
      document.getElementById('featVisuals').checked = !(data.features && data.features.visuals === false);
      document.getElementById('featPdf').checked = !(data.features && data.features.pdfExport === false);
      document.getElementById('statTotal').textContent = (data.stats && data.stats.totalRequests) || 0;
      document.getElementById('statLast').textContent = (data.stats && data.stats.lastRequestAt) || '—';
      document.getElementById('statStatus').textContent = (data.maintenance && data.maintenance.enabled) ? 'في الصيانة' : 'يعمل';
      return true;
    }

    document.getElementById('loginBtn').onclick = function() {
      loadDashboard(document.getElementById('loginSecret').value.trim());
    };

    document.getElementById('logoutBtn').onclick = function() {
      localStorage.removeItem(STORAGE_KEY);
      adminSecret = '';
      document.getElementById('appView').style.display = 'none';
      document.getElementById('loginView').style.display = 'flex';
      notify('تم تسجيل الخروج');
    };

    document.getElementById('saveAllBtn').onclick = async function() {
      var payloadModels = {};
      document.querySelectorAll('input[data-k]').forEach(function(inp) {
        var k = inp.getAttribute('data-k'), f = inp.getAttribute('data-f');
        if (!payloadModels[k]) payloadModels[k] = { ...models[k] };
        payloadModels[k][f] = inp.value;
      });
      var body = {
        models: payloadModels,
        searchApiUrl: document.getElementById('searchApiUrl').value,
        searchApiKey: document.getElementById('searchApiKey').value,
        imageApi: {
          baseUrl: document.getElementById('imgBaseUrl').value,
          modelId: document.getElementById('imgModelId').value,
          apiKey: document.getElementById('imgApiKey').value
        },
        maintenance: {
          enabled: document.getElementById('maintEnabled').checked,
          message: document.getElementById('maintMsg').value
        },
        announcement: {
          enabled: document.getElementById('annEnabled').checked,
          title: document.getElementById('annTitle').value,
          message: document.getElementById('annMsg').value
        },
        features: {
          webSearch: document.getElementById('featSearch').checked,
          visuals: document.getElementById('featVisuals').checked,
          pdfExport: document.getElementById('featPdf').checked
        }
      };
      var res = await fetch('/api/admin/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Admin-Secret': adminSecret },
        body: JSON.stringify(body)
      });
      var d = await res.json();
      if (res.ok) {
        document.getElementById('searchApiKey').value = '';
        document.getElementById('imgApiKey').value = '';
        notify('✓ تم حفظ جميع الإعدادات في Cloudflare KV');
        loadDashboard(adminSecret);
      } else {
        notify(d.error || 'فشل الحفظ');
      }
    };

    if (adminSecret) {
      loadDashboard(adminSecret);
    }
  </script>
</body>
</html>`;
