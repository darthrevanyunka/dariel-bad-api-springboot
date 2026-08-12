# Hosting the Challenge

One container, exposed to the internet via a Cloudflare Tunnel. No inbound ports to open, no reverse proxy to manage, no TLS cert to renew.

## Before you start

- Docker and Docker Compose
- A free [Cloudflare](https://dash.cloudflare.com/sign-up) account
- Fill in `.env` (copy `.env.example`) with a real `BADAPI_ADMIN_KEY`

## Option A — Quick tunnel (no domain needed)

Good for a one-off event where you don't need a memorable URL, and the default if you don't have a domain added to Cloudflare. No account, no login, no token — Cloudflare hands you a random `*.trycloudflare.com` address for as long as the tunnel container runs.

```bash
docker compose up -d --build          # start the app

docker run -d --name bad-api-quick-tunnel \
  --network container:bad-api-challenge \
  cloudflare/cloudflared:latest tunnel --url http://localhost:8080

sleep 5 && docker logs bad-api-quick-tunnel 2>&1 | grep trycloudflare.com
```

That last command prints your public URL, e.g. `https://random-words.trycloudflare.com`. Give that to participants as `CHALLENGE_URL`.

**Important**: the URL is tied to this specific container process. If `bad-api-quick-tunnel` stops or restarts — including a host reboot — Cloudflare assigns a *new random URL*, breaking anything you've already shared. Don't restart it mid-event; if you must, re-share the new URL.

Teardown:
```bash
docker stop bad-api-quick-tunnel bad-api-challenge
docker rm bad-api-quick-tunnel
```

## Option B — Named tunnel with your own domain

Better if you have a domain on Cloudflare and want a clean, reusable URL like `challenge.yourdomain.com`.

1. **Create the tunnel** in the [Zero Trust dashboard](https://one.dash.cloudflare.com/) → **Networks → Tunnels → Create a tunnel** → choose **Cloudflared** → name it (e.g. `bad-api-challenge`).
2. The dashboard shows a **connector token** on the next screen. Copy it into `.env`:
   ```
   CLOUDFLARE_TUNNEL_TOKEN=eyJ...
   ```
3. Still in the tunnel setup, go to the **Public Hostname** tab and add a route:
   - **Subdomain**: `challenge` (or whatever you like)
   - **Domain**: your domain
   - **Service Type**: `HTTP`
   - **URL**: `bad-api:8080` — this is the app container's name on the Compose network, not `localhost`
4. Save, then start everything:
   ```bash
   docker compose --profile tunnel up -d --build
   ```
5. Visit `https://challenge.yourdomain.com` to confirm it's live. Give that URL to participants as `CHALLENGE_URL`.

## What participants do with the URL

Both README.md and the reference solutions read a `CHALLENGE_URL` environment variable (falling back to `http://localhost:8080`):

```bash
export CHALLENGE_URL=https://challenge.yourdomain.com
```

## Shutting down

```bash
docker compose --profile tunnel down    # stops the tunnel too, if you used Option B
```

For Option A, just `Ctrl+C` the `docker run` command, then `docker compose down`.

## Notes on sizing

This app holds everything in memory (2,000 generated records, submissions, rate-limit state) — there's no database to provision. The single container is capped at 2 CPUs / 1GB RAM in `docker-compose.yml`, which is generous headroom for ~20 concurrent participants; see the comment there for the actual math on worst-case concurrent load (including the simulated 2-minute-hang timeout behavior). If you ever need more than one instance, note that the leaderboard and rate limits are per-instance, in-memory state — you'd need a shared store (Redis, etc.) before scaling beyond one container, which is out of scope for this event-sized setup.
