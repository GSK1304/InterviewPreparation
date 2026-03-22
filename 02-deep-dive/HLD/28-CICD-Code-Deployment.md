# 📚 System Design — Code Deployment System (CI/CD Pipeline)

---

## 🎯 Problem Statement
Design a code deployment system (like GitHub Actions, Jenkins, or GitLab CI) that automatically builds, tests, and deploys code changes from version control to production, with rollback capability and zero-downtime deployments.

---

## Step 1: Clarify Requirements

### Functional
- Trigger builds on code push / pull request / manual trigger
- Run multi-stage pipelines: build → test → security scan → deploy
- Support parallelism (multiple jobs run simultaneously)
- Artifact storage (built binaries, Docker images)
- Deployment strategies: rolling, blue-green, canary
- Rollback to previous version
- Pipeline as code (YAML config in repo)
- Notifications (Slack, email on success/failure)
- Environment management (dev, staging, production)
- Secrets management (API keys, credentials in pipelines)

### Non-Functional
- **Scale**: 100K builds/day (GitHub scale: 2M+ builds/day)
- **Latency**: Pipeline start within 5 seconds of trigger
- **Reliability**: Build failures must not affect production
- **Isolation**: Jobs must not interfere with each other
- **Security**: Secrets never logged; isolation between tenants

---

## Step 2: Estimation

```
Builds:         100K builds/day = 1.16 builds/sec avg; peak = ~10 builds/sec
Build duration: avg 10 min = 600s
Concurrent:     100K/day × 600s/86400s = ~694 concurrent builds
Worker capacity: 1 worker = 1 build → need ~700 workers minimum at peak

Artifacts:
  Docker image: avg 500MB compressed
  100K builds/day × 500MB = 50TB/day artifact storage
  Retention: 30 days → 1.5PB rolling storage

Logs:
  10MB logs per build × 100K/day = 1TB/day
  Retention: 90 days → 90TB log storage
```

---

## Step 3: API Design

```
# Pipeline triggers
POST /api/v1/pipelines/trigger
  Body: { repoId, branch, commitSha, triggeredBy: "push|pr|manual|schedule" }
  Response: { pipelineId, status: "queued", estimatedStartTime }

# Pipeline status
GET  /api/v1/pipelines/{id}
  Response: { id, status, stages: [{name, status, duration, jobs:[...]}], startTime, endTime }

GET  /api/v1/pipelines/{id}/logs?stage=test&job=unit-tests
  Response: streaming log output (SSE)

# Manual actions
POST /api/v1/pipelines/{id}/cancel
POST /api/v1/pipelines/{id}/retry
POST /api/v1/pipelines/{id}/approve      → manual approval gate

# Deployments
POST /api/v1/deployments
  Body: { pipelineId, environment: "production", strategy: "canary" }
GET  /api/v1/deployments/{id}
POST /api/v1/deployments/{id}/rollback

# Artifacts
GET  /api/v1/artifacts/{pipelineId}/{name}    → download artifact
POST /api/v1/artifacts/{pipelineId}/{name}    → upload artifact (from worker)
```

---

## Step 4: High-Level Architecture

```
Code Push
    │
    ▼
┌─────────────────┐
│  Source Control  │  (GitHub / GitLab)
│  Webhook Event   │──────────────────────────────────────┐
└─────────────────┘                                        │
                                                           ▼
                                              ┌────────────────────────┐
                                              │    Pipeline Service     │
                                              │  - Parse pipeline YAML  │
                                              │  - Create pipeline run  │
                                              │  - Enqueue jobs         │
                                              └────────────┬───────────┘
                                                           │
                                              ┌────────────▼───────────┐
                                              │      Job Queue          │
                                              │   (Kafka / Redis)       │
                                              └────────────┬───────────┘
                                                           │
                             ┌─────────────────────────────────────────┐
                             │           Worker Pool                    │
                             │  ┌────────┐ ┌────────┐ ┌────────┐       │
                             │  │Worker 1│ │Worker 2│ │Worker N│       │
                             │  │(Docker)│ │(Docker)│ │(Docker)│       │
                             │  └───┬────┘ └────────┘ └────────┘       │
                             └──────┼──────────────────────────────────┘
                                    │ run job in isolated container
                    ┌───────────────┼────────────────────────────────┐
                    │               │                                 │
             ┌──────▼──────┐ ┌──────▼──────┐                ┌───────▼──────┐
             │  Artifact   │ │   Log       │                │  Deploy      │
             │  Store (S3) │ │  Store      │                │  Service     │
             │             │ │(Elasticsearch│               │  (K8s/ECS)   │
             └─────────────┘ └─────────────┘                └──────────────┘
```

---

## Step 5: Pipeline YAML — Pipeline as Code

```yaml
# .github/workflows/deploy.yml (GitHub Actions style)
# OR .gitlab-ci.yml (GitLab CI style)
# OR Jenkinsfile (Jenkins)

name: Build and Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

stages:
  - build
  - test
  - security
  - deploy-staging
  - approve         # manual gate
  - deploy-prod

jobs:
  build:
    stage: build
    image: maven:3.9-eclipse-temurin-21
    script:
      - mvn clean package -DskipTests
    artifacts:
      paths:
        - target/*.jar
      expire_in: 1 day

  unit-tests:
    stage: test
    needs: [build]
    parallel: 4        # run 4 instances of this job in parallel
    script:
      - mvn test

  integration-tests:
    stage: test
    needs: [build]
    script:
      - mvn verify -Pintegration

  security-scan:
    stage: security
    needs: [build]
    script:
      - semgrep --config=auto src/

  deploy-staging:
    stage: deploy-staging
    needs: [unit-tests, integration-tests, security-scan]
    environment: staging
    script:
      - kubectl set image deployment/app app=$IMAGE:$CI_COMMIT_SHA

  approve-prod:
    stage: approve
    when: manual        # requires human to click "approve"
    allow_failure: false

  deploy-prod:
    stage: deploy-prod
    needs: [approve-prod]
    environment: production
    script:
      - ./deploy.sh canary 10%   # start with 10% canary
```

---

## Step 6: Worker Execution — Job Isolation

```
Each job runs in an isolated container (Docker):

Job dispatcher:
  1. Dequeue job from Kafka
  2. Pull Docker image specified in pipeline YAML
  3. Start container with:
     - Cloned repo at correct commit SHA
     - Injected secrets (from Vault, not environment variables in logs)
     - Resource limits (CPU, memory, max runtime)
     - Network isolation (no access to other jobs' data)
  4. Stream stdout/stderr to Log Store in real time
  5. Collect exit code:
     - 0 = success → mark job passed, trigger downstream jobs
     - non-zero = failure → mark job failed, stop pipeline (unless allow_failure: true)
  6. Upload artifacts to S3
  7. Release container (ephemeral — destroyed after job)

Worker scaling:
  Auto-scale based on queue depth:
    Queue depth > 100 jobs → scale up workers (Kubernetes HPA)
    Queue depth = 0 for 5 min → scale down (save cost)
  
  Spot/preemptible instances for cost savings (workers are ephemeral)
  Reserved capacity for SLA-critical pipelines
```

### Job Isolation — Security
```
Secrets injection (never in logs!):
  Pipeline YAML: uses ${{ secrets.PROD_DB_PASSWORD }}
  Worker: fetches from HashiCorp Vault at runtime
    vault kv get secret/prod/db-password → injected as env var
  Log scrubber: scan all log output, redact known secret values
  
Container isolation:
  Separate Linux namespaces per job (PID, network, filesystem)
  Read-only base filesystem (writes go to ephemeral overlay)
  No host network access (each job gets its own network namespace)
  Resource cgroups: limit CPU/RAM to prevent noisy neighbours
  
Tenant isolation (SaaS CI):
  Different organisations' jobs never share the same worker
  Separate AWS accounts per large enterprise customer
  Physical machine isolation for compliance customers (GitHub Enterprise)
```

---

## Step 7: Deployment Strategies

### Rolling Deployment
```
Replace instances one-by-one with new version:

Instance 1: v1 → v2 ✅  (wait for health check)
Instance 2: v1 → v2 ✅
Instance 3: v1 → v2 ✅

Pros: Simple, no extra infrastructure, gradual
Cons: Mixed versions serving traffic during rollout (N minutes of both v1 and v2)
Rollback: re-deploy previous image (another rolling deployment backwards)
Use: Stateless services with backward-compatible APIs
```

### Blue-Green Deployment
```
Two identical environments: Blue (current) and Green (new)

  Blue  (v1) ← 100% traffic (currently live)
  Green (v2) ← 0% traffic (new version deployed and tested here)

  Switch: LB route 100% to Green (near-instant)
  Blue becomes standby for rollback

  Rollback: LB route 100% back to Blue (instant — no redeployment)

Pros: Instant cutover, instant rollback
Cons: 2x infrastructure cost during deployment
Use: Scheduled maintenance windows, high-risk deployments
```

### Canary Deployment ✅ Most Common at Scale
```
Gradually shift traffic from old to new version:

Stage 1:  5% → new (canary), 95% → old    (monitor for 10 min)
Stage 2: 25% → new,          75% → old    (monitor for 10 min)
Stage 3: 50% → new,          50% → old    (monitor for 10 min)
Stage 4: 100% → new                        (done)

Auto-rollback triggers:
  Error rate > 1%        → automatically rollback to old
  Latency p99 > 2x old   → automatically rollback
  Success rate drop > 5% → automatically rollback

Kubernetes implementation:
  Deploy new ReplicaSet with 5% of pods
  Use weighted Ingress routing (Nginx, Istio): 5% to new pods, 95% to old
  Gradually increase replica count in new RS, decrease old RS

Pros: Catch production bugs affecting small % of users; gradual risk reduction
Cons: More complex, need feature flags for stateful changes
Used by: Netflix, Google, Facebook for every production deployment
```

### Feature Flags (Complement to Deployment)
```
Decouple deployment from feature release:
  Deploy code to 100% of servers (canary)
  Feature flag controls who SEES the feature:
    - 0% initially (dark launch — code deployed but no one sees it)
    - 1% internal users
    - 5% users in one region
    - 100% globally
  
  Tools: LaunchDarkly, Unleash, Flipt, GrowthBook
  Use: New features behind flags; gradual rollout; instant kill switch
```

---

## Step 8: Rollback System

```
Every deployment is tagged with:
  - Pipeline ID (traceability)
  - Docker image SHA (immutable)
  - Config snapshot (environment variables, secrets versions)
  - Kubernetes manifest snapshot

Rollback command:
  POST /api/v1/deployments/{id}/rollback
  
  Rollback service:
    1. Look up previous successful deployment record
    2. Re-apply its Docker image + config to Kubernetes
    3. Kubernetes rolling update (forward, but to old image)
    4. Takes ~2-5 minutes (Kubernetes pod replacement)
    
For faster rollback (< 30 seconds):
  Keep N-1 deployment active (Blue-Green standby)
  Rollback = flip LB weight (instant)
  
Database migrations:
  Hardest part of rollback!
  If new version ran DB migration → rollback code may not work with migrated schema
  Solution: Expand-and-contract pattern (backward-compatible migrations only)
    Phase 1: Add new column (nullable), deploy new code (reads both)
    Phase 2: Backfill data in new column
    Phase 3: Remove old column (after full rollout confirmed)
```

---

## Step 9: Observability in Pipelines

```
Every pipeline and job must be observable:

Metrics to track:
  Pipeline success rate (target: > 95%)
  Mean time to deployment (MTTD)
  Build time per service (alert if > 2x p50)
  Queue wait time (alert if > 5 min)
  Flaky test rate (tests that fail non-deterministically)

Flaky test detection:
  Track: test_name → pass/fail rate over last 100 runs
  If pass rate 60-95%: flag as flaky
  Flaky tests auto-quarantined: still run but don't block pipeline
  Nightly flaky test analysis report to team

Failure notifications:
  Slack: DM to committer + team channel
  Include: link to failed job, first error line, time to investigate
  Smart suppression: don't spam if same test has been failing for 3 days
  PagerDuty: only for production deployment failures
```

---

## Interview Q&A

**Q: How do you ensure a CI job can't access secrets from another organisation's job?**
A: Each job runs in an isolated container with separate Linux namespaces (PID, network, filesystem). Secrets are fetched at runtime from a secrets manager (HashiCorp Vault) scoped per organisation — cross-org access is impossible by Vault policy. Containers run under different Unix users. Network isolation prevents one job from reaching another job's localhost. Large enterprises get physically separate worker fleets.

**Q: How do you handle a test suite that takes 45 minutes — how do you speed it up?**
A: Test parallelism: split test files across N parallel jobs (GitHub Actions `matrix` strategy, pytest-xdist). Smart test selection: run only tests affected by changed files (Bazel/Buck build graph analysis, or simpler: coverage-based test selection). Test sharding: each worker gets a shard of tests. Cache: build cache and test result cache (reuse results if code unchanged). After all this, 45-min suites typically reduce to 5-10 min.

**Q: What happens when a deployment fails halfway through a canary rollout?**
A: Auto-rollback trigger fires: error rate exceeded threshold → deployment system automatically reduces canary weight back to 0%, scales down new pods, marks deployment as failed. The old version continues serving 100% of traffic. Alert sent to on-call engineer. The failed deployment is preserved for debugging (logs, traces, metrics during the canary window). Next deploy attempt requires explicit human action.

**Q: How would you design "deploy on PR merge" with required approvals?**
A: Branch protection rules: merge requires N reviewer approvals + all CI checks passing. On merge: webhook fires to Pipeline Service. Pipeline runs all stages. If deploy-to-staging passes, a "pending approval" gate is created — a Slack message is sent to the team with approve/reject buttons. Approval stored in Pipeline DB. Deploy-to-prod stage polls for approval. If approved within 24h: deploys. If not: pipeline expires and must be re-triggered.

**Q: How does GitHub Actions handle 2 million builds per day?**
A: GitHub uses a massive fleet of ephemeral runners on Azure (GitHub Actions is built on Azure). Jobs are queued in a distributed job queue. Runners poll for jobs and execute one job per runner. Auto-scaling based on queue depth. Large organisations can run self-hosted runners on their own infrastructure. The queuing system is essentially a distributed work queue (Kafka/similar) with auto-scaling consumer pools.
