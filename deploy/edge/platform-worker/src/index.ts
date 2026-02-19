import { neon, type NeonQueryFunction } from "@neondatabase/serverless";

interface Env {
  DATABASE_URL: string;
  PLATFORM_ADMIN_TOKEN: string;
  DEFAULT_ENTITLEMENT_TIER?: string;
}

type EntitlementSource = "SPONSORSHIP" | "SPONSOR_PASS" | "MANUAL";

type EntitlementStatus = "ACTIVE" | "EXPIRED" | "REVOKED";

interface EntitlementUpsertInput {
  userId: string;
  appSlug: string;
  tier: string;
  sourceType: EntitlementSource;
  sourceId: string;
  status: EntitlementStatus;
  startsAt?: string | null;
  expiresAt?: string | null;
}

function json(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8"
    }
  });
}

function badRequest(message: string): Response {
  return json({ error: message }, 400);
}

function unauthorized(): Response {
  return json({ error: "Unauthorized" }, 401);
}

function parseBearerToken(headerValue: string | null): string | null {
  if (!headerValue || !headerValue.startsWith("Bearer ")) {
    return null;
  }

  return headerValue.slice("Bearer ".length).trim();
}

function requireAdmin(request: Request, env: Env): Response | null {
  const token = parseBearerToken(request.headers.get("authorization"));
  if (!token || !env.PLATFORM_ADMIN_TOKEN || token !== env.PLATFORM_ADMIN_TOKEN) {
    return unauthorized();
  }

  return null;
}

async function readJson<T>(request: Request): Promise<T | null> {
  try {
    return (await request.json()) as T;
  } catch {
    return null;
  }
}

async function upsertEntitlement(sql: NeonQueryFunction<false, false>, input: EntitlementUpsertInput) {
  const rows = await sql`
    INSERT INTO platform_entitlements (
      user_id,
      app_slug,
      tier,
      source_type,
      source_id,
      status,
      starts_at,
      expires_at
    )
    VALUES (
      ${input.userId},
      ${input.appSlug},
      ${input.tier},
      ${input.sourceType},
      ${input.sourceId},
      ${input.status},
      COALESCE(${input.startsAt ?? null}, NOW()::text)::timestamptz,
      COALESCE(${input.expiresAt ?? null}, NULL::text)::timestamptz
    )
    ON CONFLICT (user_id, app_slug, source_type, source_id)
    DO UPDATE SET
      tier = EXCLUDED.tier,
      status = EXCLUDED.status,
      starts_at = EXCLUDED.starts_at,
      expires_at = EXCLUDED.expires_at
    RETURNING
      id,
      user_id AS "userId",
      app_slug AS "appSlug",
      tier,
      source_type AS "sourceType",
      source_id AS "sourceId",
      status,
      starts_at AS "startsAt",
      expires_at AS "expiresAt"
  `;

  return rows[0];
}

function randomPassCode(): string {
  return crypto.randomUUID().replace(/-/g, "").slice(0, 12).toUpperCase();
}

function validSlug(value: string): boolean {
  return /^[a-z0-9-]{2,50}$/.test(value);
}

function defaultTier(env: Env): string {
  return env.DEFAULT_ENTITLEMENT_TIER || "DONATION";
}

async function handleCreateOrUpdateApp(request: Request, env: Env, sql: NeonQueryFunction<false, false>): Promise<Response> {
  const authFailure = requireAdmin(request, env);
  if (authFailure) {
    return authFailure;
  }

  type Payload = {
    appSlug?: string;
    displayName?: string;
    mode?: "INTEGRATED" | "STANDALONE";
    status?: "ACTIVE" | "PAUSED" | "ARCHIVED";
    metadata?: Record<string, unknown>;
  };

  const body = await readJson<Payload>(request);
  if (!body) {
    return badRequest("Invalid JSON payload.");
  }

  const appSlug = (body.appSlug || "").trim().toLowerCase();
  const displayName = (body.displayName || "").trim();
  const mode = body.mode || "INTEGRATED";
  const status = body.status || "ACTIVE";

  if (!validSlug(appSlug)) {
    return badRequest("appSlug must match ^[a-z0-9-]{2,50}$");
  }

  if (!displayName) {
    return badRequest("displayName is required.");
  }

  const metadata = body.metadata ?? {};

  const rows = await sql`
    INSERT INTO platform_apps (
      app_slug,
      display_name,
      mode,
      status,
      metadata,
      updated_at
    )
    VALUES (
      ${appSlug},
      ${displayName},
      ${mode},
      ${status},
      ${JSON.stringify(metadata)}::jsonb,
      NOW()
    )
    ON CONFLICT (app_slug)
    DO UPDATE SET
      display_name = EXCLUDED.display_name,
      mode = EXCLUDED.mode,
      status = EXCLUDED.status,
      metadata = EXCLUDED.metadata,
      updated_at = NOW()
    RETURNING
      id,
      app_slug AS "appSlug",
      display_name AS "displayName",
      mode,
      status,
      metadata,
      updated_at AS "updatedAt"
  `;

  return json({ data: rows[0] }, 201);
}

async function handleCreateSponsorship(request: Request, env: Env, sql: NeonQueryFunction<false, false>): Promise<Response> {
  const authFailure = requireAdmin(request, env);
  if (authFailure) {
    return authFailure;
  }

  type Payload = {
    sponsorUserId?: string;
    beneficiaryUserId?: string;
    appSlug?: string;
    tier?: string;
    expiresAt?: string | null;
    notes?: string;
    autoApprove?: boolean;
  };

  const body = await readJson<Payload>(request);
  if (!body) {
    return badRequest("Invalid JSON payload.");
  }

  const sponsorUserId = (body.sponsorUserId || "").trim();
  const beneficiaryUserId = (body.beneficiaryUserId || "").trim();
  const appSlug = (body.appSlug || "").trim().toLowerCase();
  const tier = (body.tier || defaultTier(env)).trim();
  const autoApprove = body.autoApprove ?? false;

  if (!sponsorUserId || !beneficiaryUserId || !validSlug(appSlug)) {
    return badRequest("sponsorUserId, beneficiaryUserId, and valid appSlug are required.");
  }

  const status = autoApprove ? "ACTIVE" : "PENDING";

  const sponsorshipRows = await sql`
    INSERT INTO platform_sponsorships (
      sponsor_user_id,
      beneficiary_user_id,
      app_slug,
      tier,
      status,
      starts_at,
      expires_at,
      approved_by,
      approved_at,
      notes
    )
    VALUES (
      ${sponsorUserId},
      ${beneficiaryUserId},
      ${appSlug},
      ${tier},
      ${status},
      NOW(),
      COALESCE(${body.expiresAt ?? null}, NULL::text)::timestamptz,
      ${autoApprove ? "system" : null},
      ${autoApprove ? new Date().toISOString() : null},
      ${body.notes || null}
    )
    RETURNING
      id,
      sponsor_user_id AS "sponsorUserId",
      beneficiary_user_id AS "beneficiaryUserId",
      app_slug AS "appSlug",
      tier,
      status,
      starts_at AS "startsAt",
      expires_at AS "expiresAt",
      notes
  `;

  const sponsorship = sponsorshipRows[0];
  let entitlement: unknown = null;

  if (status === "ACTIVE") {
    entitlement = await upsertEntitlement(sql, {
      userId: beneficiaryUserId,
      appSlug,
      tier,
      sourceType: "SPONSORSHIP",
      sourceId: sponsorship.id,
      status: "ACTIVE",
      startsAt: sponsorship.startsAt,
      expiresAt: sponsorship.expiresAt
    });
  }

  return json({ data: { sponsorship, entitlement } }, 201);
}

async function handleApproveSponsorship(request: Request, env: Env, sql: NeonQueryFunction<false, false>, id: string): Promise<Response> {
  const authFailure = requireAdmin(request, env);
  if (authFailure) {
    return authFailure;
  }

  const sponsorshipRows = await sql`
    UPDATE platform_sponsorships
    SET status = 'ACTIVE',
        approved_by = 'admin',
        approved_at = NOW()
    WHERE id = ${id}
      AND status = 'PENDING'
    RETURNING
      id,
      beneficiary_user_id AS "beneficiaryUserId",
      app_slug AS "appSlug",
      tier,
      starts_at AS "startsAt",
      expires_at AS "expiresAt"
  `;

  if (sponsorshipRows.length === 0) {
    return json({ error: "Sponsorship not found or not pending." }, 404);
  }

  const sponsorship = sponsorshipRows[0];
  const entitlement = await upsertEntitlement(sql, {
    userId: sponsorship.beneficiaryUserId,
    appSlug: sponsorship.appSlug,
    tier: sponsorship.tier,
    sourceType: "SPONSORSHIP",
    sourceId: sponsorship.id,
    status: "ACTIVE",
    startsAt: sponsorship.startsAt,
    expiresAt: sponsorship.expiresAt
  });

  return json({ data: { sponsorship, entitlement } });
}

async function handleCreateSponsorPass(request: Request, env: Env, sql: NeonQueryFunction<false, false>): Promise<Response> {
  const authFailure = requireAdmin(request, env);
  if (authFailure) {
    return authFailure;
  }

  type Payload = {
    sponsorUserId?: string;
    appSlug?: string;
    expiresInDays?: number;
  };

  const body = await readJson<Payload>(request);
  if (!body) {
    return badRequest("Invalid JSON payload.");
  }

  const sponsorUserId = (body.sponsorUserId || "").trim();
  const appSlug = (body.appSlug || "").trim().toLowerCase();
  const expiresInDays = Number.isFinite(body.expiresInDays) ? Math.max(1, Math.floor(body.expiresInDays as number)) : 30;

  if (!sponsorUserId || !validSlug(appSlug)) {
    return badRequest("sponsorUserId and valid appSlug are required.");
  }

  const passCode = randomPassCode();

  const rows = await sql`
    INSERT INTO platform_sponsor_passes (
      app_slug,
      sponsor_user_id,
      pass_code,
      status,
      expires_at
    )
    VALUES (
      ${appSlug},
      ${sponsorUserId},
      ${passCode},
      'ISSUED',
      NOW() + (${expiresInDays}::text || ' days')::interval
    )
    RETURNING
      id,
      app_slug AS "appSlug",
      sponsor_user_id AS "sponsorUserId",
      pass_code AS "passCode",
      status,
      expires_at AS "expiresAt"
  `;

  return json({ data: rows[0] }, 201);
}

async function handleRedeemSponsorPass(request: Request, env: Env, sql: NeonQueryFunction<false, false>): Promise<Response> {
  type Payload = {
    passCode?: string;
    beneficiaryUserId?: string;
  };

  const body = await readJson<Payload>(request);
  if (!body) {
    return badRequest("Invalid JSON payload.");
  }

  const passCode = (body.passCode || "").trim().toUpperCase();
  const beneficiaryUserId = (body.beneficiaryUserId || "").trim();

  if (!passCode || !beneficiaryUserId) {
    return badRequest("passCode and beneficiaryUserId are required.");
  }

  const passRows = await sql`
    SELECT
      id,
      app_slug AS "appSlug",
      expires_at AS "expiresAt",
      status
    FROM platform_sponsor_passes
    WHERE pass_code = ${passCode}
      AND status = 'ISSUED'
      AND expires_at > NOW()
    LIMIT 1
  `;

  if (passRows.length === 0) {
    return json({ error: "Sponsor pass is invalid or expired." }, 404);
  }

  const pass = passRows[0];

  await sql`
    UPDATE platform_sponsor_passes
    SET beneficiary_user_id = ${beneficiaryUserId},
        status = 'REDEEMED',
        redeemed_at = NOW()
    WHERE id = ${pass.id}
      AND status = 'ISSUED'
  `;

  const entitlement = await upsertEntitlement(sql, {
    userId: beneficiaryUserId,
    appSlug: pass.appSlug,
    tier: defaultTier(env),
    sourceType: "SPONSOR_PASS",
    sourceId: pass.id,
    status: "ACTIVE",
    startsAt: null,
    expiresAt: pass.expiresAt
  });

  return json({ data: { passId: pass.id, entitlement } });
}

async function handleListEntitlements(sql: NeonQueryFunction<false, false>, userId: string, includeExpired: boolean): Promise<Response> {
  const rows = await sql`
    SELECT
      e.id,
      e.user_id AS "userId",
      e.app_slug AS "appSlug",
      a.display_name AS "appName",
      a.mode,
      e.tier,
      e.source_type AS "sourceType",
      e.source_id AS "sourceId",
      e.status,
      e.starts_at AS "startsAt",
      e.expires_at AS "expiresAt"
    FROM platform_entitlements e
    INNER JOIN platform_apps a
      ON a.app_slug = e.app_slug
    WHERE e.user_id = ${userId}
      AND (
        ${includeExpired}
        OR (
          e.status = 'ACTIVE'
          AND (e.expires_at IS NULL OR e.expires_at > NOW())
        )
      )
    ORDER BY e.created_at DESC
  `;

  return json({ data: rows });
}

async function handleReconcile(request: Request, env: Env, sql: NeonQueryFunction<false, false>): Promise<Response> {
  const authFailure = requireAdmin(request, env);
  if (authFailure) {
    return authFailure;
  }

  const expiredEntitlements = await sql`
    UPDATE platform_entitlements
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE'
      AND expires_at IS NOT NULL
      AND expires_at <= NOW()
    RETURNING id
  `;

  const expiredPasses = await sql`
    UPDATE platform_sponsor_passes
    SET status = 'EXPIRED'
    WHERE status = 'ISSUED'
      AND expires_at <= NOW()
    RETURNING id
  `;

  const expiredSponsorships = await sql`
    UPDATE platform_sponsorships
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE'
      AND expires_at IS NOT NULL
      AND expires_at <= NOW()
    RETURNING id
  `;

  return json({
    data: {
      entitlementsExpired: expiredEntitlements.length,
      sponsorPassesExpired: expiredPasses.length,
      sponsorshipsExpired: expiredSponsorships.length
    }
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (!env.DATABASE_URL) {
      return json({ error: "DATABASE_URL is missing." }, 500);
    }

    const sql = neon(env.DATABASE_URL);
    const url = new URL(request.url);

    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ ok: true, service: "aura-platform-edge", at: new Date().toISOString() });
      }

      if (request.method === "GET" && url.pathname === "/v1/apps") {
        const rows = await sql`
          SELECT
            app_slug AS "appSlug",
            display_name AS "displayName",
            mode,
            status,
            metadata
          FROM platform_apps
          WHERE status <> 'ARCHIVED'
          ORDER BY display_name ASC
        `;
        return json({ data: rows });
      }

      if (request.method === "POST" && url.pathname === "/v1/apps") {
        return handleCreateOrUpdateApp(request, env, sql);
      }

      if (request.method === "POST" && url.pathname === "/v1/sponsorships") {
        return handleCreateSponsorship(request, env, sql);
      }

      const approveMatch = url.pathname.match(/^\/v1\/sponsorships\/([a-f0-9-]{36})\/approve$/i);
      if (request.method === "POST" && approveMatch) {
        return handleApproveSponsorship(request, env, sql, approveMatch[1]);
      }

      if (request.method === "POST" && url.pathname === "/v1/sponsor-passes") {
        return handleCreateSponsorPass(request, env, sql);
      }

      if (request.method === "POST" && url.pathname === "/v1/sponsor-passes/redeem") {
        return handleRedeemSponsorPass(request, env, sql);
      }

      const entitlementsMatch = url.pathname.match(/^\/v1\/users\/([^/]+)\/entitlements$/);
      if (request.method === "GET" && entitlementsMatch) {
        const userId = decodeURIComponent(entitlementsMatch[1]);
        const includeExpired = url.searchParams.get("includeExpired") === "true";
        return handleListEntitlements(sql, userId, includeExpired);
      }

      if (request.method === "POST" && url.pathname === "/v1/entitlements/reconcile") {
        return handleReconcile(request, env, sql);
      }

      return json({ error: "Not found" }, 404);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unexpected error";
      return json({ error: message }, 500);
    }
  }
};
