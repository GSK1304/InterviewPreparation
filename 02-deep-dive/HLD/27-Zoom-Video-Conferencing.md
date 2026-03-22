# 📚 System Design — Video Conferencing (Zoom / Google Meet)

---

## 🎯 Problem Statement
Design a video conferencing platform that supports real-time audio/video calls between multiple participants, with screen sharing, recording, chat, and low latency across geographically distributed users.

---

## Step 1: Clarify Requirements

### Functional
- 1:1 and group video calls (up to 1000 participants)
- Audio and video streaming (real-time, low latency)
- Screen sharing
- In-meeting chat
- Meeting recording (local and cloud)
- Breakout rooms
- Virtual backgrounds
- Meeting scheduling and invites
- Reactions and polls

### Non-Functional
- **Latency**: Audio/video < 150ms end-to-end (human perception threshold)
- **Scale**: 300M daily meeting participants (Zoom peak during COVID)
- **Availability**: 99.999% — missed calls = broken trust
- **Bandwidth**: Adaptive based on network conditions (50kbps to 4Mbps per stream)
- **Quality**: Graceful degradation (audio always preserved even if video degrades)

---

## Step 2: Estimation

```
Daily participants:  300M/day
Concurrent peaks:   ~50M concurrent participants
Meeting size avg:   8 participants

Video stream:       720p = ~2Mbps; 480p = ~1Mbps; 240p = ~400kbps
Audio stream:       ~64kbps (Opus codec)

Bandwidth per participant:
  Sending:    1 video + 1 audio = ~2.1Mbps upload
  Receiving:  7 others × (video + audio) = ~14.5Mbps download
  
Total bandwidth: 50M × 2Mbps = 100Tbps ingress (massive CDN/media server capacity)

Recording storage:
  1hr meeting, 720p = ~1GB
  10M meetings/day × 1hr × 1GB = 10PB/day recorded
```

---

## Step 3: API Design

```
# Meeting management
POST /v1/meetings                        → create meeting, get meetingId + join link
GET  /v1/meetings/{id}                   → meeting details
DELETE /v1/meetings/{id}                 → end meeting
POST /v1/meetings/{id}/participants      → join meeting
DELETE /v1/meetings/{id}/participants/me → leave meeting

# Signaling (WebSocket — before media flows)
WS /v1/meetings/{id}/signal
Events:
  → peer.joined    { participantId, name }
  → peer.left      { participantId }
  → offer          { sdp, from }     ← WebRTC SDP offer
  → answer         { sdp, from }     ← WebRTC SDP answer
  → ice.candidate  { candidate }     ← ICE candidate exchange

# Chat
POST /v1/meetings/{id}/messages
GET  /v1/meetings/{id}/messages

# Recording
POST /v1/meetings/{id}/recording/start
POST /v1/meetings/{id}/recording/stop
GET  /v1/recordings/{id}/download
```

---

## Step 4: The Fundamental Architecture Choice

This is the most important decision: **how do participants exchange video/audio?**

### Option A: Peer-to-Peer (P2P) via WebRTC

```
Participant A ◄──────────────────────► Participant B
Participant A ◄──────────────────────► Participant C
Participant B ◄──────────────────────► Participant C

Full mesh: N×(N-1)/2 connections for N participants
  3 people: 3 connections
  8 people: 28 connections
  100 people: 4950 connections ← impossible for clients

Pros: No server in media path → lowest latency
Cons: Doesn't scale past 4-5 participants (client bandwidth explodes)
Use: 1:1 calls, small groups (WhatsApp video call, FaceTime)
```

### Option B: SFU (Selective Forwarding Unit) ✅ Zoom's Approach

```
All participants send ONE stream to SFU
SFU forwards each participant's stream to all others

         ┌──────────────────────────────────┐
         │              SFU                  │
         │  (Media server in data center)    │
         └────────────┬─────────────────────┘
          ▲  ▲  ▲     │ forwards streams
          │  │  │     ▼
          A  B  C   A←B,C  B←A,C  C←A,B

Pros:
  Client uploads ONE stream regardless of participant count
  SFU decides which streams to forward per participant (Simulcast)
  Low latency (server just forwards, no decode/encode)
  
Cons:
  SFU must handle all bandwidth (server-side BW = N² streams)
  Client still downloads N-1 streams (bandwidth grows with participants)
  
Use: Standard video conferencing (Zoom, Google Meet, MS Teams)
```

### Option C: MCU (Multipoint Control Unit)

```
All participants send to MCU
MCU decodes ALL streams, composites into ONE mixed stream, sends back

MCU → "Mixed" layout (gallery view pre-rendered)
Each participant receives ONE composed stream regardless of group size

Pros: Client downloads ONE stream regardless of participants
Cons: MCU does expensive CPU decode+encode; higher latency; costly
Use: Legacy systems, very large webinars, low-bandwidth participants
```

---

## Step 5: WebRTC — How the Media Flows

WebRTC is the browser/mobile standard for real-time media. Understanding it is expected in interviews.

### Signaling Phase (before media)
```
Before audio/video flows, peers must exchange session descriptions.
This is NOT part of WebRTC itself — you build the signaling server.

WebRTC uses SDP (Session Description Protocol):
  SDP Offer: "I can send H.264 video at 720p, Opus audio, at these IPs..."
  SDP Answer: "I accept H.264, I'll use these IPs to receive..."

Flow:
  1. Caller creates SDP Offer
  2. Sends Offer to Signaling Server (your WebSocket server)
  3. Signaling Server relays Offer to Callee
  4. Callee creates SDP Answer
  5. Sends Answer back via Signaling Server to Caller
  6. ICE candidate exchange (finding the best network path)
  7. Direct media connection established (P2P or via SFU)
```

### ICE (Interactive Connectivity Establishment)
```
Problem: Clients are behind NAT (home routers, firewalls) — can't connect directly

ICE uses:
  STUN server: "What's my public IP? What port did NAT assign me?"
    → Free, Google runs one: stun.l.google.com:19302
    → Works for ~80% of connections (symmetric NAT fails)
    
  TURN server: Relay when direct connection fails
    → Client A → TURN server → Client B (server in media path)
    → ~20% of connections need TURN (corporate firewalls, strict NAT)
    → Expensive: TURN handles all media bandwidth for these calls
    → Must run your own TURN servers (Twilio Stun/TURN as a service)

ICE candidate gathering:
  Browser gathers: local IPs, STUN-reflected IP, TURN relay address
  Exchanges these with peer via Signaling Server
  Tries each candidate pair → picks best working route
```

### Simulcast
```
Problem: Participant A has great internet. Participant B has poor mobile data.
         If A sends one 1080p stream, B can't receive it.

Simulcast: Sender encodes MULTIPLE quality levels simultaneously
  High:   720p @ 2.5Mbps
  Medium: 360p @ 700kbps
  Low:    180p @ 200kbps

SFU receives all 3 quality levels from A
SFU sends appropriate quality to each receiver based on their bandwidth:
  Participant B (poor connection) → receives Low (180p)
  Participant C (good connection) → receives High (720p)

Client doesn't know it's receiving a downgraded stream
SFU switches quality seamlessly as bandwidth changes
```

---

## Step 6: Media Server Architecture

```
                    ┌─────────────────────────────────────┐
                    │         TURN / STUN Servers          │
                    │   (geographically distributed)       │
                    └──────────────────┬──────────────────┘
                                       │
                    ┌──────────────────▼──────────────────┐
                    │         Signaling Servers            │
                    │   WebSocket — SDP + ICE exchange     │
                    │   Redis Pub/Sub for cross-instance   │
                    └──────────────────┬──────────────────┘
                                       │ routes to SFU
                    ┌──────────────────▼──────────────────┐
                    │           SFU Cluster                │
                    │  ┌────────┐ ┌────────┐ ┌────────┐   │
                    │  │ SFU-1  │ │ SFU-2  │ │ SFU-3  │   │
                    │  │Meeting1│ │Meeting2│ │Meeting3│   │
                    │  └────────┘ └────────┘ └────────┘   │
                    │  One SFU handles one meeting room    │
                    └──────────────────┬──────────────────┘
                                       │
                    ┌──────────────────▼──────────────────┐
                    │         Recording Service            │
                    │   Receives streams → mixes → S3     │
                    └──────────────────────────────────────┘
```

### SFU Assignment
```
Meeting created → assign to SFU instance:
  Consider: geographic region of majority participants
  SFU capacity: max streams handled per SFU (hardware dependent, ~5K participants)
  
  Meeting room registry in Redis:
    HSET meeting:{meetingId} sfu_id sfu-3  sfu_host 10.0.1.33
  
  Signaling server: participant joins → lookup SFU → connect participant to that SFU
  
  Large meetings (> SFU capacity): cascade SFUs
    SFU-1 ←→ SFU-2 (inter-SFU relay)
    Participants in US → SFU-1
    Participants in EU → SFU-2
    SFUs exchange streams with each other
    Global meetings with regional SFU clusters
```

---

## Step 7: Recording

```
Cloud recording architecture:
  SFU streams → Recording Worker (separate from SFU, no user latency impact)
  Recording Worker:
    Receives: raw RTP streams per participant
    Mixes: optional server-side composition (gallery view)
    OR stores individual tracks (for post-processing)
    Encodes: H.264/H.265 video + AAC audio → MP4 container
    Uploads: S3 in segments (don't wait until meeting ends)
    
  Post-processing pipeline (async):
    S3 upload event → Kafka → Transcoding Job
    Generate: different quality levels, transcript (speech-to-text), highlights
    Update: recording status in DB → notify user it's ready

  Storage management:
    Free tier: 1GB cloud recording
    Pro: unlimited (stored compressed, lower quality after 30 days)
    Lifecycle policy: S3 Intelligent-Tiering (auto-move to Glacier for old recordings)
```

---

## Step 8: Network Adaptation

```
Video conferencing must work on 50kbps to 100Mbps connections.

Bitrate adaptation:
  Sender uses RTCP feedback: receiver reports packet loss, jitter, RTT
  Sender adjusts bitrate: if loss > 5% → reduce by 25%
  Uses: Google Congestion Control (GCC) or REMB (Receiver Estimated Max Bitrate)

Packet loss concealment:
  Audio: Opus codec has built-in PLC (Packet Loss Concealment)
    Synthesizes missing audio from surrounding packets — < 10% loss barely noticeable
  Video: Reference frames (I-frames) allow recovery after loss burst
    Request keyframe on excessive loss

Jitter buffer:
  Packets arrive out of order due to network jitter
  Buffer: hold packets for 50-200ms, reorder, release smoothly
  Tradeoff: larger buffer = smoother video but more latency

Network priority:
  Voice packets: marked DSCP EF (Expedited Forwarding) → highest priority in routers
  Video packets: DSCP AF41 (high priority)
  Data (screen share): lower priority than voice
```

---

## Interview Q&A

**Q: What's the difference between SFU and MCU? When would you choose each?**
A: SFU forwards streams without decoding — low CPU, low latency, but clients download N-1 streams. MCU decodes and composites all streams into one — high CPU, higher latency, but clients download just one stream regardless of participant count. Use SFU for standard conferencing (Zoom, Meet). Use MCU when client bandwidth is severely limited (mobile on 2G) or for accessibility (pre-mixed streams for screen readers).

**Q: Why does Zoom feel lower latency than a standard video call?**
A: Zoom uses UDP (not TCP) for media — UDP drops packets rather than retransmitting, which eliminates TCP's head-of-line blocking. Zoom runs its own TURN servers globally with BGP anycast routing — participants connect to the nearest PoP. Zoom also uses custom codecs and aggressive jitter buffer tuning. The signaling and media servers run in the same data center, reducing coordination overhead.

**Q: How does screen sharing work differently from webcam sharing?**
A: Screen sharing captures the display buffer at the OS level (not a camera). It generates frames with very different statistics — lots of identical frames (desktop sitting still) with occasional high-change frames (scrolling, typing). This enables much more aggressive compression: skip frame if identical, use high-quality I-frames for sharp text. Screen shares typically use a different codec profile (optimized for text/graphics vs. natural video motion).

**Q: How would you design the virtual background feature?**
A: Virtual backgrounds use ML-based person segmentation (a lightweight neural network runs on-device). The model outputs a mask identifying which pixels are "person" vs. "background". This runs per-frame (typically 15-30fps) before encoding. The replacement background is composited locally before the video stream is sent to the SFU. Apple's CoreML, TensorFlow Lite, and MediaPipe are commonly used on-device inference frameworks for this.

**Q: How do you handle audio echo (hearing your own voice in the call)?**
A: Echo Cancellation (AEC) runs in the audio pipeline before transmission. The algorithm knows what audio was played through the speakers (the reference signal) and subtracts it from the microphone input. This is implemented in the WebRTC stack at the OS level (and in the browser's WebRTC implementation). When using headphones instead of speakers, AEC is unnecessary since the microphone doesn't pick up playback audio.
