# 🎤 Behavioural Round — STAR Stories & Leadership Q&A

> Your kACE work is genuinely impressive material. This file structures it into interview-ready answers.
> Format: **S**ituation → **T**ask → **A**ction → **R**esult

---

## Part 1: Your Core Stories (pre-built from kACE)

### Story 1: Scaling a Team from 2 to 12 (EM Signature Story)

**When to use:** "Tell me about a time you built a team." / "How do you hire?" / "Tell me about your leadership style."

**S:** Joined BGC Partners' kACE team in early 2023 as the second hire. The team was building a greenfield FX Options trading platform (Phoenix) to replace a legacy C++ system (Garuda) used by institutional traders at Fenics.

**T:** Scale the India engineering team to support both the modernisation effort and live production system, while maintaining the hiring bar needed for a complex fintech domain.

**A:**
- Defined the hiring profile: engineers comfortable with both modern TypeScript/React/Spring Boot AND legacy C/C++ codebases (rare combination)
- Conducted 185+ L1 technical interviews across 2025 — personally screened every candidate
- Built a structured interview process with domain-specific questions (FX Options, real-time pricing, concurrency)
- Grew the team to 12 engineers with strong screening effectiveness — maintained low false-positive rate
- Onboarded team members in parallel with active development so they could contribute from week 2

**R:** Team of 12 fully operational, delivering Phoenix features while keeping Garuda stable. Zero critical production incidents attributable to onboarding mistakes. Multiple team members now leading sub-streams independently.

**Numbers to cite:** 2 → 12 engineers, 185+ interviews conducted, ~18 months timeline.

---

### Story 2: Phoenix Modernisation — Technical Leadership Under Constraint

**When to use:** "Tell me about a large technical project you led." / "How do you handle tech debt?" / "Tell me about a time you had to make hard architectural decisions."

**S:** kACE's legacy system (Garuda) was a Windows C++ codebase with a Jam build system, 20+ years of FX Options business logic, and no unit tests. The new Phoenix platform needed to replicate and surpass it using React 19, Spring Boot (Java 21), Kafka, and WebSocket — while the old system was live in production.

**T:** Lead the technical modernisation while keeping the legacy system stable, managing dual-stack development, and delivering new features to traders who couldn't tolerate downtime.

**A:**
- Established Phoenix as the primary investment; Garuda in pure maintenance mode
- Made early architectural decisions that proved critical: JWT-based auth with three tokens (access/refresh/privileges), field-clearing system using a declarative rule engine (FIELD_CLEAR_MAPPING), WebSocket/STOMP for real-time pricing
- Built a 52-strategy × 4-leg × 200+ field pricing grid (~41,600 cells) with performance optimisations: batch `setFieldValue`, virtual rendering, custom `useFormValues` hook
- When performance degraded at scale: profiled, identified the N+1 subscription pattern, refactored to shared cache — reduced re-renders by ~95%
- Introduced `StaticCacheOrchestrator` for microservice startup pre-loading so traders didn't see loading states on first open

**R:** Phoenix in production serving live traders. Performance targets met (sub-100ms field cascade updates). Legacy Garuda decommission timeline established. Team has clear ownership of both stacks.

**Key technical decisions to mention:** Declarative clearing rules (OCP), three-token JWT (security architecture), batched state updates (React performance).

---

### Story 3: Real-Time Pricing Grid Performance Crisis

**When to use:** "Tell me about a performance problem you solved." / "Tell me about a time you went deep on a technical issue." / "Describe debugging a production issue."

**S:** The kACE pricing grid — 52 strategies × 4 legs × 200+ fields — was visibly slow when traders changed input values. Market makers complained the grid "felt broken" when recalculating live prices.

**T:** Diagnose and fix the performance issue without breaking the existing field-clearing business logic, under time pressure (traders using it daily).

**A:**
- Profiled the React render tree: discovered every field subscribed to form state independently via nested `form.Field` → 41,600 individual subscriptions triggering simultaneously
- Root cause: TanStack Form v1's `setFieldValue` called 200+ times per pricing update (one per field), each triggering a separate re-render
- Fix 1: Batched all `setFieldValue` calls — reduced from 200+ to 2 calls per pricing response. React batch update handles the rest.
- Fix 2: Built `useFormValues` hook with shared module-level cache — single subscription, all fields read from cache
- Fix 3: Added `@tanstack/react-virtual` for viewport-only rendering — only DOM elements for visible cells created
- Fix 4: Frozen pane / sticky column CSS for usability without sacrificing perf
- Added metrics: measured before/after render times; ~95% reduction in unnecessary re-renders

**R:** Grid went from "felt broken" to sub-100ms updates even on large strategies. Traders stopped raising performance complaints. Pattern now used across all grid components in Phoenix.

---

### Story 4: Auth Architecture from Scratch

**When to use:** "Tell me about a security decision you made." / "How do you approach authentication at scale?" / "Tell me about something you built that you're proud of."

**S:** Phoenix needed an auth system for a multi-tenant financial platform. Existing JWT solutions didn't meet our requirements: we needed separate token lifetimes for API access vs UI sessions vs privilege claims, plus WebSocket JWT auth and cross-tab session consistency.

**T:** Design and implement a production-grade auth architecture that satisfies fintech security requirements without a third-party auth provider.

**A:**
- Designed a three-token system: short-lived access token (15 min), longer refresh token (7 days), separate privileges token (cached, updated on role change)
- RSA key separation: access and refresh tokens use different key pairs (compromise of one doesn't expose the other)
- `familyId` cookie-based session binding at the Gateway layer — prevents refresh token reuse attacks
- WebSocket JWT auth: token passed in query param on connect, validated at handshake, heartbeat-based expiry detection
- Full Axios interceptor (`httpClient.ts`) with token refresh queuing — concurrent requests queue during refresh, all resume on completion
- JWKS endpoint for microservice key rotation without downtime (kid-based key lookup)

**R:** Auth system handling all Phoenix traffic with zero security incidents. Passed internal security review. Architecture document used as reference for other Fenics teams.

---

### Story 5: Debugging a Cross-Team Production Issue

**When to use:** "Tell me about a time you worked across teams." / "Describe a difficult debugging session." / "How do you handle ambiguous problems?"

**S:** Traders reported intermittent "session expired" errors during market hours — our most critical time. The errors were non-reproducible in dev/staging, appearing only in production under load.

**T:** Identify root cause without being able to reproduce it, under pressure from trading desk, involving both frontend and backend services.

**A:**
- Started with structured hypothesis elimination: ruled out token expiry mismatch, network timeout, and frontend bug sequentially
- Built a Node.js SSL proxy with Jasypt decryption and tshark-based HTTP/WebSocket capture scripts to intercept production traffic safely
- Discovered: under concurrent requests, the refresh token queue had a race condition — two requests both detected an expired token simultaneously, both initiated refresh, the first succeeded, the second used a now-invalidated refresh token (leading to forced logout)
- Fix: mutex on the refresh queue in `httpClient.ts` — first requester refreshes, all others wait and resume with the new token
- Added distributed tracing with request IDs across Gateway → Auth Service → App Service to catch future cross-service issues

**R:** Issue resolved and never recurred. The tooling built (proxy, capture scripts) became part of the team's debugging toolkit. Reduced mean time to debug WebSocket issues from days to hours.

---

## Part 2: Common Behavioural Questions with Frameworks

### "Tell me about yourself"
Structure: Current role (2-3 sentences) → Technical journey (1-2 sentences) → What you're looking for (1 sentence)

*"I'm a Technical Manager at BGC Partners, leading the kACE India team — we build the Phoenix FX Options trading platform used by institutional traders at Fenics/Cantor Fitzgerald. I joined as the second engineer, scaled the team to 12 through 185+ interviews, and have been leading both the modernisation of a legacy C++ system and the greenfield React/Java platform simultaneously. I'm now looking for a role where I can continue operating at the IC + EM intersection — deep technical work alongside team building and architectural leadership."*

---

### "What's your greatest weakness?"
**Framework:** Real weakness → What you've done about it → Evidence it's improving

*"Early in my EM role I struggled to delegate technical decisions — I'd step in and solve problems myself rather than letting team members work through them, which limited their growth and created a bottleneck. I've deliberately worked on this by setting a personal rule: if a problem is solvable by someone on my team, I ask guiding questions rather than providing answers. Concretely, two engineers on my team now lead design reviews independently — something that wouldn't have happened if I'd kept solving their problems for them."*

---

### "Tell me about a conflict with a team member / stakeholder"
**Framework:** What was at stake → Your approach → Resolution → Relationship outcome

*"A senior engineer on my team strongly disagreed with my decision to build the field-clearing system as a declarative rule engine (FIELD_CLEAR_MAPPING) rather than a procedural one. He felt the abstraction was over-engineering for the current scale. I took his concern seriously — we held a design review, I laid out the extensibility requirements (52 strategies with different clearing rules, expected to grow), and acknowledged his concern about complexity. We agreed on a middle path: build the declarative engine but with explicit documentation and a fallback escape hatch. Six months later, when we added a new strategy type, he told me the decision had saved us a week of work. The key was not overruling him but making the reasoning visible."*

---

### "Tell me about a time you failed"
**Framework:** What happened → What you missed → What you changed → Specific outcome from the change

*"When we launched the first version of the real-time pricing WebSocket, I didn't account for multi-instance scaling — I'd focused on getting it working, not on how it would behave when we added more pods. In production we discovered that RFQ updates fired on one pod weren't reaching clients connected to other pods. The fix (Redis Pub/Sub via SubscriptionRegistry) was straightforward, but it caused an unplanned outage during market hours. What I changed: now all distributed components have a written 'multi-instance behaviour' section in the design doc before we code. It's a checklist item in our PR template."*

---

### "Why do you want to leave your current role?"
*"I've had a genuinely great run at BGC — built something from scratch, scaled a team, shipped a complex fintech platform. I'm proud of what we built. What I'm looking for now is a broader scope — either a larger organisation where the engineering problems are at a different scale, or a product where the domain complexity is matched by the technical challenge. kACE is deep in FX Options, which is a narrow domain. I want to apply the same technical depth to problems with wider impact."*

---

## Part 3: EM-Specific Questions

**Q: How do you balance IC work and management as your team grows?**
A: At 12 reports, I spend roughly 40% on IC (architecture decisions, critical code reviews, hard debugging), 40% on management (1:1s, hiring, removing blockers), and 20% on strategy (roadmap, stakeholder alignment). The IC work is non-negotiable for me — a manager who can't read the code loses the team's respect and makes bad tradeoffs. I protect IC time by batching meetings and keeping Monday mornings code-only. As teams grow past ~8 direct reports I believe you need to either hire a senior IC anchor or create sub-leads — you can't maintain both quality and velocity without that.

**Q: How do you handle an underperforming engineer?**
A: First understand root cause — is it skill gap, motivation, personal circumstances, or wrong role? I've had all four. My approach: explicit, specific, timely feedback (not vague). Agree on a short improvement window (2-4 weeks) with measurable outcomes. Weekly check-ins with documented progress. If it's a skill gap and the person is motivated, pair programming and explicit mentorship. If after two cycles there's no improvement, move to a formal process. The kindest thing is clarity — vague feedback wastes everyone's time and doesn't give the engineer a fair chance.

**Q: How do you make architectural decisions with a team that has strong opinions?**
A: I use a structured decision process: (1) Write down the options and criteria before the meeting — this forces me to think clearly and gives the team something to react to rather than starting from blank. (2) Explicit time-box for dissent: "you have until end of day to raise concerns in writing." (3) For reversible decisions: decide fast, build in a review checkpoint. For irreversible decisions: spend more time, do a pre-mortem. The key is making the decision-making process legible so people feel heard even when they disagree with the outcome. I explicitly separate "your idea was heard" from "your idea was chosen" — they're different things.

**Q: How do you maintain technical standards as a team scales?**
A: Three mechanisms: (1) Written standards — we have a documented coding standards doc that covers Money-in-paise, no `any` in TypeScript, constructor validation patterns. New joiners read it in week one. (2) PR culture — senior engineers rotate as reviewers, not just direct manager. Standards don't stick if only one person enforces them. (3) Pattern libraries — I extract common patterns into shared utilities (our `httpClient.ts`, `useFormValues` hook) so the right way is also the easy way. Engineers don't violate standards out of malice; they do it out of convenience. Remove the convenience gap.

**Q: Tell me about a time you pushed back on a product decision.**
A: Product wanted us to add real-time notifications for every RFQ state change — 50+ event types — in a single sprint. I pushed back. Not because it was technically impossible, but because we'd just shipped the WebSocket layer and adding that event volume without proper back-pressure handling and client-side deduplication would destabilise what we'd just built. I laid out the risk explicitly: "If we ship this now, we'll spend the next sprint firefighting." Proposed instead: ship 5 critical notifications now (the ones traders actually need), build the full system properly in sprint +2. Product agreed. The proper system we built in sprint +2 handled all 50+ events cleanly — the rushed version wouldn't have.

---

## Part 4: Questions to Ask the Interviewer

**For IC roles:**
- "What does the on-call rotation look like for this team, and how often are there P0 incidents?"
- "How are architectural decisions made — is there an RFC process or a design review culture?"
- "What does the codebase look like in terms of test coverage and tech debt? What's the biggest source of developer frustration?"
- "What would success look like for this role in the first 6 months?"

**For EM roles:**
- "What's the team's current biggest engineering challenge — technical, organisational, or both?"
- "How is the EM role positioned relative to product — are engineers involved in roadmap decisions?"
- "What does the career path look like for engineers on this team? Any recent promotions?"
- "What's the one thing about this team's culture that would surprise an outsider?"

**For both:**
- "What does your strongest engineer do differently from your average engineer?"
- "What's a recent technical decision the team made that you're proud of?"
- "Is there anything about my background that gives you pause? I'd rather address it now."

---

## Part 5: Salary Negotiation Anchors

**Never accept the first number without a counter.** Always ask for time: "Can I have 24 hours to review this?"

**Counteroffer framework:**
- Research market rate first (levels.fyi, Glassdoor, LinkedIn Salary for your title/city)
- Anchor above your target: ask for 10-15% more than your floor
- Justify with specifics: "Based on my research for this level in Hyderabad, and given that I'm bringing both EM and IC depth, I was expecting X"
- If base is fixed: negotiate signing bonus, equity vesting acceleration, remote flexibility, title

**Phrases that work:**
- "I'm very interested in this role. My current compensation is X. Based on the scope of this role, I was expecting Y."
- "Is there flexibility on the total comp structure? I'm open to a mix of base and equity."
- "What's the typical range for this level? I want to make sure we're aligned before going further."
