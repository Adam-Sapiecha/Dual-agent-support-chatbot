# Deployment: luxonis

Demo URL: https://luxonis.tail6ad453.ts.net:8443/support/

Runtime: Java 17 ARM64, Docker Compose. Source release:
`/home/pi/apps/dual-agent-support/releases/20261021-1515`.
Only the application port `127.0.0.1:8091` is bound on the host.
Public HTTPS uses the existing Tailscale Funnel on 8443, handler `/support`.
Other handlers and the private Nextcloud service are preserved.
The application works at `/` locally and uses relative assets/API URLs when
the proxy strips `/support`. Always share the address with the trailing slash.

## Local reproduction

```powershell
.\mvnw.cmd clean verify
java -jar target/dual-agent-support.jar
# http://localhost:8080/
```

Or `docker compose up -d --build`. No API key is needed or forwarded.

## Server update

Copy source (pom, src, docs, dockerfile, compose.yaml, .dockerignore) into a new
release folder, then run from that folder:

```sh
sudo env APP_VERSION=your-release-tag docker compose build
sudo env APP_VERSION=your-release-tag docker compose up -d
curl --fail http://127.0.0.1:8091/api/health
sudo tailscale funnel --bg --https=8443 --set-path=/support http://127.0.0.1:8091
```

Use `funnel`, not `serve`, when updating this public port: `serve` disables Funnel
for the port. The existing port 8443 is already public through Funnel. Do not reset Tailscale
Serve or expose port 443; that hosts private services. To replicate elsewhere,
configure HTTPS on your own reverse proxy, strip the chosen base path, and
forward only to this loopback port. Never forward API credentials to the web UI.
Persist `APP_VERSION=your-release-tag` in the release directory's `.env` so
later Compose commands keep selecting the same image. The deployed directory
contains this version-only configuration; no API credentials are stored there.

## Operations and rollback

```sh
cd /home/pi/apps/dual-agent-support/releases/20261021-1515
sudo docker compose ps
sudo docker compose logs --tail=80 web
sudo docker compose stop web
sudo docker compose start web
```

Use the New conversation button to delete one session. State expires after
30 idle minutes (cleanup within another minute). Restarting `web` clears all
sample state. No persistent volumes are required.

Before upgrading, tag the current image under a distinct version. To roll back,
run Compose in the previous source directory with its previous `APP_VERSION`
and `up -d --no-build`. For this first deployment the pre-existing state had
no support application: stop this Compose project and remove only its handler
with `sudo tailscale funnel --https=8443 --set-path=/support off`, then verify
`tailscale funnel status` still lists the existing root and `/taskmesh` routes
as public. If needed, re-enable the existing root target with
`sudo tailscale funnel --bg --https=8443 http://127.0.0.1:8087`.
The original Serve configuration is backed up on the server at
`/home/pi/apps/dual-agent-support/backups/tailscale-before-20261021.json`.
Do not restore the entire backup over unrelated newer changes.

## Limits

Compose requests 512 MiB container RAM, 1 CPU, 100 PIDs, read-only filesystem with a 64 MiB
temporary directory, non-root UID 10001, no Linux capabilities, no-new-privileges,
bounded log files, health check and restart policy. The API accepts up to 2000
characters and a 12000-byte JSON request, at most 120 chat requests/minute
globally, 300 active sessions and 30 turns/session. Browser requests time out
after 15 seconds. Requests are processed serially in the small in-memory demo.

The Pi kernel currently reports that Docker memory limits are unsupported, so
the requested 512 MiB is **not enforced on this host**. Java therefore has explicit
limits independent of cgroups: 256 MiB heap, 128 MiB metaspace, 64 MiB code cache
and 32 MiB direct buffers. Native stacks and other runtime memory are additional;
these JVM settings are not a hard limit on total process RSS. Enable memory
cgroups separately if a strict aggregate container limit is required.

## Verification record

Published on 21 October 2026. Public HTTPS returns HTTP 200 for the UI and
`{"mode":"sample","status":"ok"}` for `/support/api/health`. The container
healthcheck passes. Public API verification completed the three-turn sample
refund and deletion of its session. Browser QA exercised technical routing,
the refund buttons and New conversation, on desktop and at a 390×844 viewport.
The mobile DOM had equal viewport and document widths (no horizontal overflow).
Screenshots are in `docs/screenshots/`.

GitHub Pages served the updated project entry with the actual support demo
link, and its deployment workflow completed successfully (portfolio commit
`ce379ee`). The portfolio update was based on the latest remote main in a
temporary worktree, preserving unrelated local edits and remote changes.

The final ARM64 image build executes all 19 tests. The preceding source/image
versions remain available under release directories `20261021-1503` and
`20261021-1510` for rollback. No full host restart was performed; existing
services were checked separately after deployment.
