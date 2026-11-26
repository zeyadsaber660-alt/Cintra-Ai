# Cintra AI — Cloudflare Worker Backend & API Dashboard

هذا المجلد يحتوي على السيرفر الآمن (Cloudflare Worker) ولوحة التحكم الزجاجية المصفّرة (`/dashboard`) لإدارة مفاتيح الـ API بعيداً عن تطبيق الأندرويد.

## خطوات النشر السريع

1. أنشئ قاعدة بيانات KV لحفظ إعدادات النماذج ومفاتيح الـ API:
   ```bash
   wrangler kv namespace create CINTRA_CONFIG
   ```
   انسخ الـ `id` الناتج وضعه في `wrangler.toml`.

2. أضف كلمتي السر (مرة واحدة فقط):
   ```bash
   wrangler secret put ADMIN_SECRET
   wrangler secret put APP_TOKEN
   ```

3. انشر الـ Worker:
   ```bash
   wrangler deploy
   ```

4. افتح رابط الـ Worker في المتصفح (`/dashboard`) وأدخل `ADMIN_SECRET` لإضافة مفاتيح OpenAI / DeepSeek / Qwen / Gemini لكل نموذج من نماذج Cintra الأربعة.
5. داخل تطبيق **Cintra** على الأندرويد: افتح القائمة الجانبية ← **ربط سيرفر الـ API (Worker)** ← أدخل رابط الـ Worker و`APP_TOKEN`.
