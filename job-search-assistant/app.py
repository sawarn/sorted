#!/usr/bin/env python3
"""Local-first job search assistant. Run with: python3 app.py"""

from __future__ import annotations

import html
import json
import os
import re
import sqlite3
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DB_PATH = ROOT / "job_search.sqlite3"
HOST = "127.0.0.1"
PORT = int(os.environ.get("JOB_ASSISTANT_PORT", "8765"))


def connect_db():
    db = sqlite3.connect(DB_PATH, timeout=10)
    db.row_factory = sqlite3.Row
    return db


def init_db():
    with connect_db() as db:
        db.execute("CREATE TABLE IF NOT EXISTS profile (id INTEGER PRIMARY KEY CHECK (id=1), data TEXT NOT NULL)")
        db.execute("CREATE TABLE IF NOT EXISTS boards (id INTEGER PRIMARY KEY AUTOINCREMENT, provider TEXT NOT NULL, company TEXT NOT NULL, token TEXT NOT NULL, UNIQUE(provider, token))")
        db.execute("""CREATE TABLE IF NOT EXISTS jobs (
            id TEXT PRIMARY KEY, provider TEXT NOT NULL, company TEXT NOT NULL, title TEXT NOT NULL,
            location TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, apply_url TEXT NOT NULL,
            posted TEXT NOT NULL, score INTEGER NOT NULL, matched TEXT NOT NULL, found_at TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'match'
        )""")
        db.execute("CREATE TABLE IF NOT EXISTS packets (job_id TEXT PRIMARY KEY, data TEXT NOT NULL, updated_at TEXT NOT NULL)")
        db.execute("INSERT OR IGNORE INTO profile(id, data) VALUES (1, '{}')")


def plain_text(value):
    value = re.sub(r"(?is)<(script|style).*?>.*?</\1>", " ", value or "")
    value = re.sub(r"(?s)<[^>]+>", " ", value)
    return re.sub(r"\s+", " ", html.unescape(value)).strip()


def https_url(value):
    value = str(value or "")
    parsed = urllib.parse.urlparse(value)
    return value if parsed.scheme == "https" and parsed.netloc else ""


def safe_token(token):
    token = token.strip()
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,100}", token):
        raise ValueError("Use the company board name only (letters, numbers, dots, underscores, or hyphens).")
    return token


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": "LocalJobSearchAssistant/0.1", "Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=18) as response:
        return json.loads(response.read().decode("utf-8"))


def normalize_board(board):
    provider, company, token = board["provider"], board["company"], safe_token(board["token"])
    if provider == "greenhouse":
        rows = fetch_json(f"https://boards-api.greenhouse.io/v1/boards/{urllib.parse.quote(token)}/jobs?content=true").get("jobs", [])
        normalized = []
        for row in rows:
            normalized.append({
                "key": str(row.get("id", "")), "title": row.get("title", ""),
                "location": (row.get("location") or {}).get("name", ""),
                "description": plain_text(row.get("content", "")),
                "url": https_url(row.get("absolute_url", "")), "apply_url": https_url(row.get("absolute_url", "")),
                "posted": row.get("updated_at", ""),
            })
        return normalized
    if provider == "lever":
        rows = fetch_json(f"https://api.lever.co/v0/postings/{urllib.parse.quote(token)}?mode=json")
        normalized = []
        for row in rows:
            cats = row.get("categories") or {}
            normalized.append({
                "key": str(row.get("id", "")), "title": row.get("text", ""),
                "location": cats.get("location", "") or ", ".join(cats.get("allLocations") or []),
                "description": plain_text(row.get("descriptionPlain") or row.get("description", "")),
                "url": https_url(row.get("hostedUrl", "")), "apply_url": https_url(row.get("applyUrl", "") or row.get("hostedUrl", "")),
                "posted": row.get("createdAt", ""),
            })
        return normalized
    raise ValueError("Choose Greenhouse or Lever.")


def score_job(job, profile):
    haystack = (job["title"] + " " + job["description"]).casefold()
    skills = [s.strip() for s in re.split(r"[,\n]", profile.get("skills", "")) if s.strip()]
    desired = [s.strip() for s in re.split(r"[,\n]", profile.get("target_roles", "")) if s.strip()]
    avoid = [s.strip() for s in re.split(r"[,\n]", profile.get("avoid_terms", "")) if s.strip()]
    matched = [s for s in skills if s.casefold() in haystack]
    title_matches = [s for s in desired if s.casefold() in job["title"].casefold()]
    stopwords = {"about", "after", "also", "and", "are", "been", "both", "build", "built", "clients", "company", "create", "created", "data", "design", "develop", "developed", "during", "each", "from", "have", "into", "including", "lead", "led", "more", "other", "over", "project", "projects", "responsible", "role", "several", "that", "their", "this", "through", "using", "with", "work", "worked", "year", "years"}
    resume_tokens = set(re.findall(r"[a-z][a-z0-9+#.]{2,}", profile.get("resume_text", "").casefold())) - stopwords
    resume_matches = sorted(token for token in resume_tokens if token in haystack and token not in {s.casefold() for s in matched})[:20]
    score = min(55, len(matched) * 11) + min(25, len(resume_matches) * 2) + min(35, len(title_matches) * 18)
    location_pref = profile.get("location", "").strip().casefold()
    if location_pref and (location_pref in job["location"].casefold() or "remote" in job["location"].casefold()):
        score = min(100, score + 10)
    if any(term.casefold() in haystack for term in avoid):
        score = max(0, score - 35)
    return min(100, score), matched + resume_matches + title_matches


class Handler(BaseHTTPRequestHandler):
    server_version = "JobAssistant/0.1"

    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))

    def send_json(self, value, status=200):
        body = json.dumps(value).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def read_json(self):
        length = int(self.headers.get("Content-Length", "0"))
        if length > 1_000_000:
            raise ValueError("Request is too large.")
        return json.loads(self.rfile.read(length) or b"{}")

    def do_GET(self):
        if self.path == "/api/bootstrap":
            with connect_db() as db:
                profile = json.loads(db.execute("SELECT data FROM profile WHERE id=1").fetchone()[0])
                boards = [dict(r) for r in db.execute("SELECT * FROM boards ORDER BY company COLLATE NOCASE")]
                jobs = [dict(r) for r in db.execute("SELECT * FROM jobs ORDER BY score DESC, found_at DESC")]
                packets = {r["job_id"]: json.loads(r["data"]) for r in db.execute("SELECT job_id, data FROM packets")}
            return self.send_json({"profile": profile, "boards": boards, "jobs": jobs, "packets": packets})
        if self.path in ("/", "/index.html"):
            body = (ROOT / "index.html").read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            return self.wfile.write(body)
        self.send_error(404)

    def do_POST(self):
        try:
            payload = self.read_json()
            if self.path == "/api/profile":
                allowed = ("name", "email", "phone", "location", "linkedin", "portfolio", "work_authorization", "sponsorship", "salary", "target_roles", "skills", "avoid_terms", "resume_text")
                profile = {key: str(payload.get(key, "")).strip()[:20000] for key in allowed}
                with connect_db() as db:
                    db.execute("UPDATE profile SET data=? WHERE id=1", (json.dumps(profile),))
                return self.send_json({"ok": True})
            if self.path == "/api/boards":
                provider = str(payload.get("provider", "")).lower()
                company = str(payload.get("company", "")).strip()[:100]
                token = safe_token(str(payload.get("token", "")))
                if provider not in ("greenhouse", "lever") or not company:
                    raise ValueError("Add a company name and choose a supported job board.")
                with connect_db() as db:
                    db.execute("INSERT OR IGNORE INTO boards(provider, company, token) VALUES (?, ?, ?)", (provider, company, token))
                return self.send_json({"ok": True})
            if self.path == "/api/boards/delete":
                with connect_db() as db:
                    db.execute("DELETE FROM boards WHERE id=?", (int(payload.get("id", 0)),))
                return self.send_json({"ok": True})
            if self.path == "/api/search":
                return self.search()
            if self.path == "/api/status":
                job_id, status = str(payload.get("job_id", "")), str(payload.get("status", "match"))
                if status not in ("match", "review", "applied", "closed"):
                    raise ValueError("Invalid application status.")
                with connect_db() as db:
                    db.execute("UPDATE jobs SET status=? WHERE id=?", (status, job_id))
                return self.send_json({"ok": True})
            if self.path == "/api/packet":
                job_id = str(payload.get("job_id", ""))[:300]
                data = payload.get("data", {})
                if not job_id or not isinstance(data, dict):
                    raise ValueError("Choose a job and enter an application draft.")
                packed = json.dumps({key: str(value)[:20000] for key, value in data.items()})
                with connect_db() as db:
                    db.execute("INSERT INTO packets(job_id,data,updated_at) VALUES(?,?,?) ON CONFLICT(job_id) DO UPDATE SET data=excluded.data,updated_at=excluded.updated_at", (job_id, packed, datetime.now(timezone.utc).isoformat()))
                return self.send_json({"ok": True})
            self.send_error(404)
        except (ValueError, json.JSONDecodeError) as exc:
            return self.send_json({"error": str(exc)}, 400)
        except Exception as exc:
            return self.send_json({"error": str(exc)}, 500)

    def search(self):
        with connect_db() as db:
            profile = json.loads(db.execute("SELECT data FROM profile WHERE id=1").fetchone()[0])
            boards = [dict(r) for r in db.execute("SELECT * FROM boards ORDER BY company")]
        if not boards:
            return self.send_json({"error": "Add at least one company career board first."}, 400)
        found, errors = 0, []
        timestamp = datetime.now(timezone.utc).isoformat()
        for board in boards:
            try:
                rows = normalize_board(board)
                for row in rows:
                    if not row["key"] or not row["title"]:
                        continue
                    score, matched = score_job(row, profile)
                    job_id = f"{board['provider']}:{board['token']}:{row['key']}"
                    with connect_db() as db:
                        db.execute("""INSERT INTO jobs(id,provider,company,title,location,description,url,apply_url,posted,score,matched,found_at)
                            VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET
                            title=excluded.title, location=excluded.location, description=excluded.description,
                            url=excluded.url, apply_url=excluded.apply_url, posted=excluded.posted,
                            score=excluded.score, matched=excluded.matched, found_at=excluded.found_at""",
                            (job_id, board["provider"], board["company"], row["title"], row["location"], row["description"][:12000], row["url"], row["apply_url"], str(row["posted"]), score, json.dumps(matched), timestamp))
                    found += 1
            except (urllib.error.URLError, TimeoutError, ValueError, json.JSONDecodeError) as exc:
                errors.append({"company": board["company"], "error": str(exc)[:240]})
        with connect_db() as db:
            jobs = [dict(r) for r in db.execute("SELECT * FROM jobs ORDER BY score DESC, found_at DESC")]
        return self.send_json({"found": found, "errors": errors, "jobs": jobs})


if __name__ == "__main__":
    init_db()
    print(f"Job Search Assistant is running at http://{HOST}:{PORT}")
    print("Your profile, resume text, saved jobs, and application status stay in this folder.")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
