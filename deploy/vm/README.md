# VM Deployment (Single Linux Host + Docker)

This path is for the lowest predictable always-on monthly cost with standard Linux operations.

## What this stack includes

- Caddy reverse proxy with automatic TLS (Let's Encrypt)
- AURA backend container
- MariaDB container

All services run on one VM through `deploy/vm/docker-compose.vm.yml`.

## 1) Prepare the VM

Install Docker Engine + Docker Compose plugin.

## 2) Configure environment

```bash
cd deploy/vm
cp .env.example .env
# Edit .env values
```

## 3) Start or update

```bash
./deploy.sh
```

## 4) Verify

```bash
docker compose --env-file .env -f docker-compose.vm.yml ps
docker compose --env-file .env -f docker-compose.vm.yml logs -f app
```

## Notes

- `Caddyfile` already supports wildcard app hosts (`*.apps.example.com`) and forwards `X-App-Host`.
- This repo's existing compose files remain valid for local and legacy deployments.
- For shared-login multi-app setups, use the platform schema in `deploy/shared/sql/001_platform_core.sql` and partition by `app_slug`.
