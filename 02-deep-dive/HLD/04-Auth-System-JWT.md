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
