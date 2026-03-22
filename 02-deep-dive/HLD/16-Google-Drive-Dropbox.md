# 📚 System Design — Cloud File Storage (Google Drive / Dropbox)

---

## 🎯 Problem Statement
Design a cloud file storage service where users can upload, download, sync files across devices, share with others, and access previous versions.

---

## Step 1: Clarify Requirements

### Functional
- Upload and download files (any type, up to 5GB per file)
- Sync files across multiple devices in near real-time
- Share files/folders with other users (view/edit permissions)
- File versioning (restore to previous version)
- Folder hierarchy (directory structure)
- Search files by name
- Offline access (local cache on device)
- Collaborative editing (Google Docs integration — out of scope here)

### Non-Functional
- **Scale**: 1B users, 500M DAU, avg 10GB storage per user
- **Availability**: 99.99% (data must never be lost — durability > everything)
- **Consistency**: Strong for metadata; eventual for file sync across devices
- **Latency**: Upload starts within 1s; download starts within 2s
- **Storage**: 1B × 10GB = 10 exabytes total

---

## Step 2: Estimation

```
Storage:     1B users × 10GB = 10 EB total
             500M DAU × 10 file changes/day = 5B file operations/day = 57K QPS

Upload BW:   500M DAU × 1 upload/day × avg 1MB = 500TB/day = ~5.8 GB/s
Download BW: 500M DAU × 3 downloads/day × avg 1MB = 1.5PB/day = ~17.4 GB/s

Metadata:    Each file: ~500 bytes (name, size, type, owner, versions)
             1B users × 100 files = 100B files × 500B = 50TB metadata
```

---

## Step 3: API Design

```
# File operations
POST   /api/v1/files/initUpload           → get upload URL + uploadId
PUT    /api/v1/files/upload/{uploadId}    → upload chunk
POST   /api/v1/files/upload/{uploadId}/complete → finalize upload
GET    /api/v1/files/{id}                → file metadata
GET    /api/v1/files/{id}/download       → pre-signed download URL
DELETE /api/v1/files/{id}               → delete (soft delete)

# Folder operations
POST   /api/v1/folders                  → create folder
GET    /api/v1/folders/{id}/contents    → list folder contents

# Sharing
POST   /api/v1/files/{id}/share         → share with user {userId, permission}
GET    /api/v1/files/{id}/permissions   → list shares

# Versions
GET    /api/v1/files/{id}/versions      → version history
POST   /api/v1/files/{id}/restore/{versionId} → restore version

# Sync
GET    /api/v1/sync/changes?since={timestamp}&deviceId={id}
       → delta of changes since last sync
```

---

## Step 4: High-Level Architecture

```
                    ┌────────────────────────────────────────┐
                    │         Client Devices                  │
                    │  Desktop | Mobile | Web Browser         │
                    │  [Local Sync Client]                    │
                    └─────────┬──────────────────────────────┘
                              │ Chunked uploads + sync delta
                    ┌─────────▼──────────────────────────────┐
                    │          API Gateway                    │
                    └──────┬──────────┬──────────────────────┘
                           │          │
              ┌────────────▼──┐  ┌────▼──────────────┐
              │  Metadata Svc │  │  Upload/Download   │
              │               │  │  Service           │
              └────────┬──────┘  └────┬───────────────┘
                       │              │
              ┌────────▼──────────────▼─────┐
              │      Object Storage          │
              │   (S3/GCS — block storage)  │
              └─────────────────────────────┘
                       │
              ┌────────▼──────────────────┐
              │   CDN (downloads)          │
              │   (CloudFront/Akamai)      │
              └───────────────────────────┘
                       │
              ┌────────▼──────────────────┐
              │   Metadata DB              │
              │   (MySQL + replicas)       │
              │   Redis (sync state)       │
              └───────────────────────────┘
```

---

## Step 5: File Chunking — The Key Design Decision

### Why Chunk Files?
```
5GB file upload over unreliable network:
  Without chunking: upload fails at 80% → restart from 0 → terrible UX
  With chunking: upload fails at chunk 400/500 → resume from chunk 401

Benefits:
  - Resumable uploads (restart from failed chunk)
  - Parallel upload (multiple chunks simultaneously)
  - Delta sync (only upload changed chunks on modification)
  - Deduplication (same chunk content = same hash = store once)
```

### Chunk-Based Upload Flow
```
File: report.pdf (100MB)

Step 1: Client splits into 4MB chunks:
  chunk_1: bytes 0–4MB, hash = sha256(data) = "abc..."
  chunk_2: bytes 4–8MB, hash = "def..."
  ...
  chunk_25: bytes 96–100MB, hash = "xyz..."

Step 2: Client asks server: "Which chunks do you already have?"
  POST /files/initUpload { chunkHashes: ["abc...", "def...", ...] }
  Server returns: { missingChunks: [3, 7, 22, ...] }

Step 3: Upload only missing chunks (deduplication + bandwidth saving!)
  PUT /files/upload/{uploadId}/chunk/3 → binary data
  PUT /files/upload/{uploadId}/chunk/7 → binary data

Step 4: Finalize
  POST /files/upload/{uploadId}/complete { fileId, chunkManifest }
  Server assembles: S3 multipart complete

On file MODIFICATION (sync efficiency):
  Only chunks that changed → only upload those chunks
  Example: edit line 50 in a 1GB file → only 1-2 chunks changed
  Dropbox reported 90%+ bandwidth savings from delta sync
```

### Chunk Storage in S3
```
S3 key pattern:  chunks/{sha256_hash}/{first2chars}/{next2chars}/{hash}
  e.g., chunks/abc123.../ab/c1/abc123...

Content-addressable storage:
  Same content = same hash = stored ONCE regardless of how many files use it
  Deduplication across ALL users globally

Manifest stored in metadata DB:
  file_id → [chunk_1_hash, chunk_2_hash, ..., chunk_N_hash]
```

---

## Step 6: Metadata Database Schema

```sql
CREATE TABLE files (
    id          UUID PRIMARY KEY,
    owner_id    BIGINT,
    parent_id   UUID,       -- parent folder (null = root)
    name        VARCHAR(255),
    file_type   VARCHAR(50),
    size_bytes  BIGINT,
    is_folder   BOOLEAN,
    is_deleted  BOOLEAN DEFAULT FALSE,
    current_version_id UUID,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE file_versions (
    id          UUID PRIMARY KEY,
    file_id     UUID REFERENCES files(id),
    version_num INT,
    size_bytes  BIGINT,
    chunk_manifest JSONB,    -- [{seq: 1, hash: "abc..."}, ...]
    created_at  TIMESTAMP,
    created_by  BIGINT       -- who made this version
);

CREATE TABLE file_shares (
    file_id     UUID,
    shared_with BIGINT,      -- userId
    permission  ENUM('VIEW', 'EDIT', 'COMMENT'),
    share_link  VARCHAR(100) UNIQUE, -- for link sharing
    expires_at  TIMESTAMP,
    PRIMARY KEY (file_id, shared_with)
);

CREATE TABLE device_sync_state (
    device_id   VARCHAR(100),
    user_id     BIGINT,
    last_sync_at TIMESTAMP,
    PRIMARY KEY (device_id)
);
```

---

## Step 7: Sync Protocol

### Delta Sync (Only Transfer Changes)
```
Goal: User edits a file on Laptop → changes appear on Phone within seconds

Step 1: Laptop detects file change (OS file watcher)
Step 2: Compute changed chunks (compare checksums)
Step 3: Upload only changed chunks to server
Step 4: Update metadata DB: new file_version record
Step 5: Server publishes to Kafka: file.updated {fileId, userId, timestamp}
Step 6: Sync Service pushes notification to all user's other devices
Step 7: Phone wakes up sync client
Step 8: Phone requests delta: GET /sync/changes?since={lastSyncTimestamp}
Step 9: Phone downloads changed chunks from CDN

Conflict resolution:
  Both laptop and phone edit the same file offline:
  - Server detects conflict (two versions from same base version)
  - Create conflict copy: "report (John's conflicted copy 2025-01-01).pdf"
  - User resolves manually (like Dropbox)
  OR: Operational Transformation (like Google Docs — much more complex)
```

### Long Polling / WebSocket for Real-time Sync
```
Client maintains persistent connection for sync notifications:
  WebSocket or SSE: server pushes "file changed" events immediately
  Client fetches delta → applies locally

For offline clients:
  They request delta when they come online: GET /sync/changes?since=lastSync
  Server returns list of changed file IDs since that timestamp
```

---

## Step 8: File Versioning

```
Version storage:
  Every save creates a new version_id in file_versions
  Old versions reference old chunk hashes in S3
  Chunks themselves are immutable and content-addressed

Retention policy:
  Free tier: 30-day version history
  Pro tier: 180-day version history
  Business: Unlimited version history

Cleanup:
  Background job: delete version records older than retention period
  If a chunk hash is no longer referenced by ANY version → delete from S3 (GC)
  Reference counting: chunk_references (chunk_hash, count)
  
Cost optimization:
  Most versions differ by only a few chunks → minimal extra storage
  S3 Intelligent Tiering: old versions moved to cheaper storage automatically
```

---

## Step 9: Security

```
Client-side encryption (zero-knowledge):
  File encrypted on client before upload
  Server never sees plaintext (true privacy)
  BUT: search, preview, collaboration become impossible
  Used by: Tresorit, ProtonDrive (privacy-first products)

Server-side encryption (standard):
  S3 AES-256 encryption at rest
  TLS in transit
  Key management via AWS KMS
  Used by: Dropbox, Google Drive, OneDrive (most cloud storage)

Per-user key encryption:
  Each user has a master key (derived from password)
  File encryption keys wrapped with master key
  Allows key rotation without re-encrypting all files

Access control on download:
  Pre-signed S3 URLs with 15-minute expiry
  Never expose raw S3 URLs to clients
  Check permissions in metadata DB before generating pre-signed URL
```

---

## Interview Q&A

**Q: How do you handle files larger than S3's 5TB object limit?**
A: Chunk the file into 4MB pieces, store each as a separate S3 object, maintain the manifest in the DB. Reassemble at download time. S3 multipart upload supports up to 10,000 parts × 100MB each = 1TB in one S3 object. For multi-TB files, use multiple S3 objects with a manifest layer.

**Q: How do you implement sharing with a link (no login required)?**
A: Generate a random token (32 bytes, URL-safe base64), store in `file_shares.share_link`. Link: `https://drive.example.com/share/{token}`. On access: validate token, check expiry, check permissions. For sensitive files: require password (hash stored alongside token) or add expiry date.

**Q: How does Dropbox's delta sync achieve such high efficiency?**
A: Content-defined chunking (variable-size chunks based on content, not fixed 4MB) — boundaries align with content changes, so editing one paragraph doesn't invalidate all subsequent chunks. Rolling hash (Rabin fingerprinting) to find chunk boundaries. This is why Dropbox reports 90%+ bandwidth savings over simple whole-file upload.

**Q: What's the difference between your design and Google Drive's design?**
A: Google Drive uses Google's Colossus distributed filesystem instead of S3. Google Docs enables collaborative real-time editing (Operational Transformation). Google uses their internal infrastructure (Bigtable for metadata, Colossus for storage). The fundamental chunk + metadata + CDN architecture is similar, but at 10x the scale.

**Q: How would you design offline access with conflict resolution?**
A: Local client maintains a SQLite DB of file metadata and a local cache of recently accessed files. Changes tracked as a local delta log. On reconnect: client sends local delta to server, server merges with server delta since last sync. Conflicts (both sides changed same file) → create conflict copy, notify user. Three-way merge for text files if full conflict resolution needed.
