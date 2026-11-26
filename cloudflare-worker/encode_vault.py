#!/usr/bin/env python3
"""
Helper script for the Cintra developer to XOR-obfuscate the Cloudflare Worker URL
and App Token into Kotlin IntArrays for `CintraSecureVault.kt`.

Usage:
    python3 cloudflare-worker/encode_vault.py https://cintra-api.example.workers.dev my-secret-app-token
"""
import sys

MASK_KEY = 0x5A

def encode(text: str) -> str:
    vals = [str(ord(ch) ^ ((MASK_KEY + (i * 7)) & 0xFF)) for i, ch in enumerate(text)]
    return f"intArrayOf({', '.join(vals)})"

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 encode_vault.py <WORKER_HTTPS_URL> <APP_TOKEN>")
        sys.exit(1)
    url, token = sys.argv[1].strip().rstrip("/"), sys.argv[2].strip()
    print("// Paste these two lines into app/src/main/java/com/cintra/app/data/security/CintraSecureVault.kt:")
    print(f"private val WORKER_URL_BYTES: IntArray = {encode(url)}")
    print(f"private val APP_TOKEN_BYTES: IntArray = {encode(token)}")
