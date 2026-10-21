# Dual-Agent Support System · Relay

A Java support conversation with technical and billing specialists. Originally a
recruitment task, expanded with a browser demo, a REST API and regression tests.

**[Open the public demo](https://luxonis.tail6ad453.ts.net:8443/support/)**

## What to try

1. Choose **API authentication** to see a TECH route and cited X-API-Key rules.
2. Ask **How much is the Pro plan?** to switch to BILLING and query sample prices.
3. Start a new conversation → **I want a refund** → `recruiter@example.com` →
   `I was charged twice`. The system collects fields and creates a DEMO case.
4. Try a pancake recipe to exercise the out-of-scope path, then reset.

The public demo runs real Java request handling and business tools with **sample
data and deterministic rules**. It does not call an LLM, send email, refund money
or store cases in an external system. Use fictional data. Mostly English
keywords are supported; arbitrary natural language is not understood by this mode.

## Architecture

Browser → REST API → bounded session state → keyword router → technical document
search / validated billing tools → answer, decision trail, tool names and sources.

Technical demo replies quote actual sections from the four support files in
`docs/`. Search scores words and headings; no relevant section means a clarification.
Billing tools provide a fictional catalog, refund timelines and simulated cases.
Sessions expire after 30 idle minutes (cleanup within a minute), hold no persistent
chat log, and can be deleted with New conversation. Refresh starts a new browser
conversation; the old server state expires automatically.

The separate **CLI** retains the original OpenAI router, technical agent with
embedding retrieval, and billing agent with function calling. It needs an API key.
Live API behavior was not verified during this audit; local regression tests cover
tool message sequencing and cache invalidation. Prompt instructions alone are not
a formal guarantee of grounding or injection resistance.

## Run and test

Java 17+ required. From the repository root:

```powershell
.\mvnw.cmd clean verify
java -jar target/dual-agent-support.jar
```

Open http://localhost:8080/ . Linux/macOS: `bash mvnw clean verify`.
Change the HTTP port through the `PORT` environment variable.

Docker (supports ARM64):

```sh
docker compose up -d --build
docker compose ps
# http://localhost:8091/
docker compose down
```

Compose builds and runs tests, binds only to loopback, limits resources, and uses a
non-root read-only runtime. Copy `.env.example` to `.env` only to change its port or
image tag. Public web mode requires no secrets and never uses BILLING_API_BASE_URL.

## Original CLI with OpenAI

```powershell
$env:OPENAI_API_KEY="your-key"
$env:OPENAI_MODEL="gpt-4o-mini"
.\mvnw.cmd compile exec:java
```

Exit with `exit` or `quit`. Never commit the key. `OPENAI_MODEL` defaults to
`gpt-4o` in the CLI. Optional `BILLING_API_BASE_URL` connects the CLI tools to
an external HTTP billing service; leave it unset for simulated tools. The console
mode is for trusted local use and is not exposed by the public web server.

## HTTP contract

- `GET /api/health` → status + `mode: sample`.
- `GET /api/docs` → public support documentation sections.
- `POST /api/chat`, JSON `{"message":"HTTP 401","sessionId":null}` →
  opaque sessionId, route, reason, answer, sources, tools, mode and turn count.
  Return the sessionId on subsequent turns.
- `DELETE /api/session`, JSON `{"sessionId":"..."}` → removes that state.

Chat limits: 2000 characters, 12000-byte request, 30 turns/session, 300 active
sessions, 120 messages/minute globally. Invalid input returns 400; expired or
unknown session 410; exceeded limits 429; full session capacity 503. Do not share
session IDs. No endpoint lists conversations or exposes administrative actions.

## Audit and operating notes

- [Audit and verification](docs/project-audit.md)
- [Server deployment, update and rollback](docs/deployment.md)
- [Portfolio description PL/EN](docs/portfolio.md)
- [Machine-readable portfolio metadata](portfolio.json)

19 automated tests cover technical retrieval, prices, multi-turn refunds,
isolation, reset, invalid dates, input and rate limits, HTTP contracts, tool
sequencing, router fallback and embedding cache changes. No LLM quality benchmark,
paid API integration test or production billing integration is claimed.

Author: Adam Sapiecha.
