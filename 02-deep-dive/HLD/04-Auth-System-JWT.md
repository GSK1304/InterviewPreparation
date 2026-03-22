# 📚 HLD Deep Dive — Authentication & Authorization System (JWT)

---

## 🎯 Problem Statement
Design a secure authentication and authorization system for a distributed microservices platform supporting millions of users, multiple client types (web, mobile, API), and role-based access control.

---

## Step 1: Clarify Requirements

### Functional
- User registration and login (username/password, OAuth2/SSO)
- Session management (tokens, expiry, revocation)
- Role-based access control (RBAC) — different permissions per role
- Multi-device support (user logged in on multiple devices)
- Token refresh without re-login
- Logout (single device and all devices)
- MFA support (optional)

### Non-Functional
- **Latency**: Auth check < 5ms per request (every API call goes through this)
- **Scale**: 100M users, 100K auth requests/sec
- **Security**: Token theft protection, replay attack prevention
- **Availability**: 99.99% — auth down = all services down

---

## Step 2: JWT Deep Dive

### JWT Structure
```
Header.Payload.Signature

Header:  base64url({ "alg": "RS256", "typ": "JWT" })
Payload: base64url({
    "sub": "user123",        // subject (user ID)
    "iat": 1700000000,       // issued at
    "exp": 1700000900,       // expiry (15 min)
    "roles": ["TRADER", "VIEWER"],
    "jti": "unique-token-id" // JWT ID (for revocation)
})
Signature: RS256(header + "." + payload, privateKey)
```

### Symmetric (HMAC) vs Asymmetric (RSA/ECDSA)
| | HS256 (HMAC) | RS256 (RSA) |
|--|--------------|-------------|
| Key | Single shared secret | Private key signs, public key verifies |
| Who can verify | Anyone with secret | Anyone with public key (no secret needed) |
| Use | Monolith, single service | Microservices (services only need public key) |
| Key rotation | Tricky (invalidates all tokens) | Rotate private key, distribute new public key |
| **kACE approach** | ❌ | ✅ — each microservice verifies with public key |

---

## Step 3: Three-Token Architecture (kACE Pattern)

```
Access Token:
  - Short-lived (15 min)
  - Contains: userId, roles, permissions
  - Signed with RSA private key
  - Sent in Authorization header: "Bearer <token>"
  - Stateless verification at each service

Refresh Token:
  - Long-lived (7 days)
  - Stored in HttpOnly Secure cookie (not accessible to JS)
  - Stored in DB (allows revocation)
  - Used ONLY to get new access token

Privileges Token (kACE-specific):
  - Contains fine-grained permissions (which FX products user can trade)
  - Separate from roles for performance (avoids DB lookup per request)
  - Refreshed when permissions change
  - Signed with separate RSA key pair (separation of concerns)
```

### Token Refresh Flow
```
1. Access token expires (15 min)
2. Client sends refresh token (in HttpOnly cookie) to /auth/refresh
3. Auth Service:
   a. Validates refresh token (check DB — not revoked, not expired)
   b. Issues new access token (15 min)
   c. Optionally rotate refresh token (sliding window expiry)
4. Client uses new access token

Refresh token rotation:
  - On each refresh, old refresh token is invalidated, new one issued
  - Protects against token theft (stolen token only valid once)
```

---

## Step 4: High-Level Architecture

```
               ┌─────────────────────────────────────────┐
               │                 Clients                  │
               │  Web (React) | Mobile | External API     │
               └──────────────────┬──────────────────────┘
                                  │
               ┌──────────────────▼──────────────────────┐
               │           API Gateway                    │
               │  - JWT verification (public key)         │
               │  - Rate limiting per user                │
               │  - Route to microservices                │
               │  - familyId cookie binding               │
               └─────┬───────────┬────────────┬──────────┘
                     │           │            │
          ┌──────────▼───┐ ┌─────▼────┐ ┌────▼──────┐
          │ Auth Service │ │ Pricing  │ │ RFQ Svc   │
          │  - Login     │ │ Service  │ │           │
          │  - Register  │ └──────────┘ └───────────┘
          │  - Refresh   │
          │  - Revoke    │
          └──────┬───────┘
                 │
     ┌───────────┼───────────┐
     ▼           ▼           ▼
 Auth DB     Token Store  JWKS Endpoint
 (Postgres)  (Redis)      (public keys)
```

### JWKS Endpoint (JSON Web Key Set)
```
Each microservice fetches public keys from:
  GET /auth/.well-known/jwks.json
  Response: { "keys": [{ "kty": "RSA", "kid": "key-1", "n": "...", "e": "AQAB" }] }

Services cache JWKS locally with TTL (refresh every 1 hour)
→ No call to Auth Service on every request
→ Supports key rotation: new key published alongside old for overlap period
```

---

## Step 5: Session Management & Revocation

### Problem: JWT is Stateless — How to Revoke?
```
Option 1: Short TTL (15 min)
  - Tokens auto-expire quickly
  - Acceptable for most use cases
  - Stolen token valid for at most 15 min

Option 2: Token Denylist (Redis)
  - On logout: add jti to Redis denylist (TTL = token's remaining lifetime)
  - On each request: check denylist (adds ~1ms Redis lookup)
  - Redis SET jti:abc123 "revoked" EX 900

Option 3: Token versioning
  - Store token_version in DB per user
  - JWT contains token_version claim
  - On logout: increment user's token_version
  - On verify: check JWT version == DB version
  - Problem: requires DB lookup on every request

✅ kACE approach: Short TTL (15 min) + Redis denylist for explicit logout
```

### familyId Cookie Binding (kACE-specific)
```
Problem: Refresh token theft (attacker steals cookie)

Solution: Bind refresh token to browser session via familyId
  - On first login: generate familyId, store in DB + cookie
  - Each refresh token is linked to a familyId
  - If refresh token used with wrong familyId → revoke entire family (all sessions)
  - Detects token theft: if attacker and victim both try to use → conflict → revoke

Implementation:
  DB: refresh_tokens(jti, user_id, family_id, expires_at, revoked)
  On refresh: verify family_id matches cookie; rotate token; update DB
```

---

## Step 6: Database Schema

```sql
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,  -- bcrypt hash
    mfa_secret   VARCHAR(100),            -- TOTP secret
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    jti          UUID PRIMARY KEY,
    user_id      UUID REFERENCES users(id),
    family_id    UUID NOT NULL,
    expires_at   TIMESTAMP NOT NULL,
    revoked      BOOLEAN DEFAULT FALSE,
    revoked_at   TIMESTAMP,
    created_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE roles (
    id   UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE  -- TRADER, VIEWER, ADMIN
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role_id UUID REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- Redis: token denylist
-- SETEX jti:{jti} {remaining_ttl_seconds} "revoked"
-- SETEX user:{userId}:server {heartbeat_ttl} {chatServerId}
```

---

## Step 7: WebSocket Authentication (kACE Pattern)

```
Problem: WebSocket handshake is HTTP — send JWT in query param or subprotocol header
  ?token=... in URL (logged in server access logs — security risk)

Better approach (kACE):
  1. Client gets short-lived WebSocket token (1 min TTL) from /auth/ws-token
  2. Client connects: ws://api/ws?wsToken=...
  3. Gateway validates wsToken → upgrades to WebSocket
  4. Connection is established; wsToken is one-time use
  5. Heartbeat-based expiry: client sends ping every 30s, gateway validates JWT periodically

Heartbeat-based JWT refresh for WebSocket:
  - Client sends heartbeat with current access token
  - If token expires soon (< 5 min), server sends new token via WebSocket message
  - Client updates token for future REST calls
```

---

## Step 8: OAuth2 / SSO Integration

```
Authorization Code Flow (Web):
  1. User clicks "Login with Google"
  2. Redirect to Google OAuth: ?client_id=...&redirect_uri=...&scope=email
  3. User authenticates on Google
  4. Google redirects back: /callback?code=abc123
  5. Backend exchanges code for tokens: POST /oauth/token {code, client_secret}
  6. Backend creates/updates local user, issues own JWT
  7. Client uses our JWT (not Google's token) for all subsequent calls

Why issue our own JWT:
  - Control token lifetime and claims
  - Not dependent on Google's token format/revocation
  - Works same as password auth for all services
```

---

## Step 9: Security Best Practices

```
Passwords:
  - bcrypt with cost factor 12 (not MD5/SHA — computationally expensive by design)
  - Never store plaintext; never return in API responses

Tokens:
  - Access token: Authorization header (never in URL, never in localStorage)
  - Refresh token: HttpOnly Secure SameSite=Strict cookie (not accessible to JS)
  - CSRF protection: SameSite cookie + CSRF token for sensitive operations

Transport:
  - TLS everywhere (even internal service-to-service)
  - Certificate pinning for mobile apps

Rate Limiting:
  - Login endpoint: 5 attempts per IP per 15 min → account lockout
  - Token refresh: 10 per user per hour
  - Registration: 3 per IP per hour

Input Validation:
  - Validate JWT alg field (prevent "alg: none" attack)
  - Validate exp, iat, iss, aud claims
  - Reject JWTs signed with weak keys
```

---

## Step 10: Trade-offs

| Decision | Choice | Reason |
|----------|--------|--------|
| Token type | JWT (stateless) | No session store lookup per request |
| Signing | RSA (asymmetric) | Microservices verify without shared secret |
| Access TTL | 15 min | Balance security and UX |
| Refresh storage | DB + Redis | DB for audit; Redis for fast revocation check |
| Cookie type | HttpOnly Secure | Prevents XSS token theft |
| Revocation | Denylist + short TTL | Simple, fast, no global state needed |
| WebSocket auth | One-time token | Avoids long-lived token in URL |

---

## Interview Q&A

**Q: JWT is stateless — what are the tradeoffs versus stateful sessions?**
A: JWT (stateless): no DB lookup per request (fast), horizontally scalable (any server validates), but cannot be revoked before expiry without a denylist (adding statefulness). Session tokens (stateful): stored in DB or Redis, can be revoked instantly, but require a lookup on every request (adds latency) and a centralised session store (potential SPOF). The right choice: use JWT with short TTL (15 min) for microservices — the performance benefit outweighs the revocation limitation. If instant revocation is required (security breach response), add a Redis denylist keyed by JTI. Don't use JWT for sessions that must be instantly revocable without this extra layer.

**Q: How do you rotate signing keys without invalidating all existing tokens?**
A: Use the `kid` (key ID) field in the JWT header. Maintain multiple active keys simultaneously. When rotating: (1) Generate new key pair (new `kid`). (2) Publish both old and new public keys in the JWKS endpoint for an overlap period (e.g., 1 week). (3) Start signing new tokens with the new private key. (4) Old tokens (signed with old key) continue to validate — their `kid` points to the old public key still in JWKS. (5) After token TTL has passed (all old tokens expired), remove the old key from JWKS. (6) Clients/services cache JWKS with a short TTL (1 hr) and re-fetch when they encounter an unknown `kid`. Zero downtime rotation.

**Q: What is the OAuth2 PKCE flow and when do you use it instead of the standard auth code flow?**
A: PKCE (Proof Key for Code Exchange) is for public clients that cannot safely store a client_secret — mobile apps, SPAs, CLI tools. Standard auth code flow uses `client_secret` to exchange the auth code for tokens. Mobile apps can't keep secrets (anyone can decompile the app). PKCE replaces the secret with: (1) App generates a random `code_verifier` and its hash `code_challenge`. (2) Sends `code_challenge` in the auth request. (3) Exchanges code using the original `code_verifier` (only the legitimate app knows it). (4) Server verifies hash matches. No secret stored in the app. Use PKCE for all public clients; use standard auth code flow for confidential server-side clients.

**Q: How do you implement SSO (Single Sign-On) across multiple applications?**
A: Central Identity Provider (IdP) — Okta, Auth0, or self-built. Flow: (1) User tries to access App A, not authenticated. (2) App A redirects to IdP: `idp.example.com/auth?client_id=app-a&redirect_uri=...`. (3) User authenticates at IdP (once). (4) IdP issues an authorization code, redirects to App A. (5) App A exchanges code for tokens (contains user identity). (6) User tries to access App B — IdP checks its own session cookie — already authenticated — skips login — issues tokens for App B. The IdP session cookie is the "single sign-on" mechanism. Single sign-out: logging out of IdP invalidates the session cookie → all apps must re-authenticate.

**Q: How would you design MFA (Multi-Factor Authentication)?**
A: After password verification, if user has MFA enabled: generate a 6-digit TOTP (Time-based One-Time Password) using the HMAC-SHA1 of (secret_key + floor(now/30)) — changes every 30 seconds. The user's authenticator app (Google Authenticator, Authy) generates the same code from the same secret. Server validates the code ± 1 time window (for clock drift). Recovery codes: generate 10 random 8-character codes at MFA setup time, store bcrypt hashes, show to user once to save. For SMS-based OTP: generate a random 6-digit code, store in Redis with TTL=300s, send via Twilio, validate on submission. TOTP is more secure than SMS (SIM swap attacks), so prefer TOTP when possible.
