# 📚 AI/ML System Design — LLM Inference Serving

---

## 1. What Makes LLM Inference Different

LLM inference is fundamentally different from serving a traditional ML model (like an image classifier). Understanding why is key to designing at scale.

```
Traditional ML (image classification):
  Input:  1 image → single forward pass → output: [0.9 cat, 0.1 dog]
  Latency: 5-20ms
  Throughput: 1000 requests/sec on one GPU
  Stateless: each request is independent

LLM inference:
  Input:  prompt (N tokens) → iterative token generation → output: M tokens
  Latency: 500ms-5s (depends on output length)
  Throughput: 10-100 requests/sec on one GPU (much lower!)
  Stateful: generation is sequential (each token depends on all previous)
  
  Two phases:
    Prefill:  process the full input prompt (parallelisable, fast)
    Decode:   generate tokens one-at-a-time (sequential, the bottleneck)
  
  Memory hungry:
    Model weights:      70B params × 2 bytes (FP16) = 140GB
    KV Cache per req:   ~1MB per 1K tokens context
    1000 concurrent requests × 1MB = 1TB KV cache ← the real constraint
```

---

## 2. GPU Architecture for LLM Serving

```
GPU types for inference:
  NVIDIA A100 (80GB): $10-15K/card, gold standard, 312 TFLOPS
  NVIDIA H100 (80GB): $30-40K/card, 2x A100, transformer-optimised
  NVIDIA L40S (48GB): $10K, strong cost/performance for smaller models

Memory constraints:
  Model weights + KV cache must fit in GPU VRAM
  70B model (FP16) = 140GB → needs 2× A100 (80GB each) minimum
  
  With quantisation:
    INT8 quantisation: 70B → 70GB → fits on 1× A100
    INT4 quantisation: 70B → 35GB → fits on 1× smaller GPU
    Quality impact: INT8 negligible; INT4 noticeable on complex tasks

Tensor parallelism:
  Split model across N GPUs (different layers on different GPUs)
  70B model on 2× A100: each GPU holds 35GB of weights
  Communication overhead between GPUs: NVLink (fast, same server)

Pipeline parallelism:
  Different layers on different servers
  Pipeline stages run in parallel on different requests
  Higher GPU utilisation for large models
  Used by: vLLM, TensorRT-LLM at scale
```

---

## 3. KV Cache — The Key to Throughput

```
During generation, attention scores for previous tokens are recomputed
every iteration unless cached.

Without KV Cache:
  Generating token 100 requires re-computing attention for tokens 1-99
  Cost: O(n²) per token → gets exponentially slow for long sequences

With KV Cache:
  Store the Key (K) and Value (V) matrices for every past token
  When generating token 100: K and V for tokens 1-99 already stored
  Only compute attention for the new token
  Cost: O(n) per token — much better!

Memory cost of KV cache:
  Per token per layer: 2 (K+V) × num_heads × head_dim × 2 bytes (FP16)
  For Llama-2-70B: 2 × 64 × 128 × 2 = 32,768 bytes = 32KB per token per layer
  80 layers → 32KB × 80 = 2.56MB per token
  1K token context → 2.56GB KV cache for ONE request

This is why batching is hard:
  Each concurrent request needs its own KV cache
  10 concurrent 1K-token requests = 25.6GB KV cache alone
  KV cache often limits concurrency more than compute does

Paged Attention (vLLM innovation):
  KV cache managed like OS virtual memory
  Allocated in fixed-size "pages" (not contiguous)
  Shared prefix pages between requests with same system prompt
  Result: 2-4× throughput improvement vs naive KV cache management
  Used by: vLLM (open-source), widely adopted
```

---

## 4. Batching Strategies

```
Naive batching:
  Wait for N requests, process as one batch
  Problem: variable request arrival → either wait too long or batch too small
  Throughput waste: GPU idle waiting to fill the batch

Continuous batching (iteration-level batching):
  Also called dynamic batching or in-flight batching
  
  Insight: different requests finish at different times
  
  Naive: process batch of 4 requests → all 4 must finish before next batch
  
  Continuous: as soon as one request finishes, add a new one
    Batch is always full (maximise GPU utilisation)
    New requests start immediately without waiting for the full batch
    
  Result: 5-10× higher throughput vs naive batching
  Used by: vLLM, TGI (Text Generation Inference), TensorRT-LLM
  
Chunked prefill:
  Long prompts monopolise the GPU during prefill
  Solution: interleave prefill chunks with decode iterations
  Allows short requests to get tokens faster even while long prompts are loading
```

---

## 5. Quantisation in Practice

```
Full precision (FP32): 4 bytes per parameter → rarely used in inference
Half precision (FP16): 2 bytes per parameter → standard for inference
BFloat16 (BF16): 2 bytes, better numerical range → preferred on newer GPUs

INT8 quantisation:
  1 byte per parameter → 2× memory reduction
  Algorithm: GPTQ, AWQ (post-training quantisation)
  Quality: nearly identical for most tasks
  Speed: 1.5-2× faster (INT8 arithmetic is faster than FP16 on some hardware)
  
INT4 quantisation:
  0.5 bytes per parameter → 4× memory reduction vs FP16
  Quality: noticeable degradation on complex reasoning, math
  Use: running large models on consumer GPUs, edge deployment
  
GGUF format (llama.cpp):
  Mixed quantisation (Q4_K_M, Q5_K_M, etc.)
  Optimised for CPU inference
  Allows running 7B models on laptops (8GB RAM)

Production recommendation:
  API serving: FP16 (quality matters, have the hardware)
  Cost-optimised: INT8 GPTQ (minimal quality loss, half the cost)
  Self-hosted / edge: INT4 GGUF (acceptable quality, very low resource)
```

---

## 6. Speculative Decoding

```
Problem: Decoding is sequential (one token at a time) → GPU mostly idle between tokens

Insight: A small "draft" model generates N tokens fast
         The large "target" model verifies all N tokens in ONE forward pass

Algorithm:
  1. Small model (7B) speculatively generates 4 tokens: [the, cat, sat, on]
  2. Large model (70B) runs ONE forward pass to verify all 4 tokens simultaneously
  3. Large model accepts tokens up to the first mismatch
  4. If all 4 accepted: 4 tokens generated for cost of 1 large model pass!
  5. If mismatch at token 3: accept tokens 1-2, discard 3-4, continue from token 3

Average speedup: 2-3× token generation speed
Requirement: small and large model from same "family" (similar distributions)
Used by: Google Gemini, Anthropic Claude (internally), DeepMind
```

---

## 7. Streaming — Critical for UX

```
Without streaming:
  User waits 3-5 seconds staring at a blank screen
  Then full response appears at once
  
With streaming:
  Tokens appear as generated (~20-50ms between tokens at generation speed)
  User sees text filling in progressively
  Perceived as much faster even if total time is the same

Implementation: SSE (Server-Sent Events)

Server side:
  POST /v1/chat/completions { stream: true }
  
  for each generated token:
    yield f"data: {json.dumps({'delta': {'content': token}})}\n\n"
  
  yield "data: [DONE]\n\n"

Client side:
  const eventSource = new EventSource('/v1/chat/completions');
  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') return;
    const token = JSON.parse(event.data).delta.content;
    appendToDisplay(token);
  };

First Token Time (TTFT):
  Most important latency metric for streaming
  = time from request to first visible token
  Target: < 500ms (feels responsive)
  
Token throughput (tokens/sec after first token):
  Target: > 20 tokens/sec (human reading speed)
  < 10 tokens/sec: noticeably slow, text trickles in
  > 50 tokens/sec: faster than user can read
```

---

## 8. Model Routing and Cost Optimisation

```
Not all queries need the most expensive model.
Intelligent routing saves 60-80% of inference cost.

Request classification:
  Simple factual: "What is 2+2?" → GPT-3.5/Haiku (cheap, fast)
  Moderate: "Write an email to..." → Sonnet/GPT-4o-mini
  Complex: "Analyse this legal document..." → Claude Opus/GPT-4

Router implementation:
  Option 1: Rule-based (keyword heuristics, query length)
    Fast, predictable, no extra cost
    
  Option 2: Small classifier model
    Train a small model (BERT-small) to classify complexity
    < 5ms overhead, 85%+ accuracy
    
  Option 3: LLM-as-router (LiteLLM routing)
    Ask a cheap model: "Rate this query complexity: simple/medium/complex"
    Very accurate but adds latency and cost
    
LiteLLM:
  Open-source unified API across providers (OpenAI, Anthropic, Cohere, Bedrock)
  Route by: cost, latency, accuracy, load balancing
  Fallback: if primary provider down → failover to secondary

Prompt caching:
  OpenAI: automatic caching for prompts > 1024 tokens (50% cost discount)
  Anthropic: explicit cache_control headers (5-min or 1-hr TTL)
  Pattern: put static content (system prompt, reference docs) at START of prompt
           put dynamic content (user message) at END
  System prompt = 2000 tokens sent every request × 1M requests/day = huge savings

Semantic caching:
  Cache LLM responses by semantic similarity of the query
  "What's the capital of France?" and "Tell me the capital of France" → same answer
  Redis: store (embedding_hash → response)
  Hit rate: 20-40% for typical chatbot traffic (many repeated question patterns)
  Tool: GPTCache, Semantic Kernel cache
```

---

## 9. Design: ChatGPT-Style Conversational AI Service

### Requirements
```
Functional:
  Multi-turn conversations (context preserved across messages)
  Streaming responses
  System prompt customisation per deployment
  Conversation history retrieval
  Multiple models (cheap/expensive routing)
  File/image attachment support
  Usage tracking per user/org

Non-Functional:
  Scale: 10M users, 100M messages/day = 1,157 messages/sec
  Latency: TTFT < 500ms; full response < 10s
  Availability: 99.99%
  Cost: optimise per-query spend
```

### Architecture

```
                        Client (Web/Mobile)
                             │ HTTPS + SSE
                    ┌────────▼────────────┐
                    │    API Gateway       │
                    │  JWT auth, rate limit│
                    └────────┬────────────┘
                             │
                    ┌────────▼────────────┐
                    │   Chat Service       │
                    │  - Validate request  │
                    │  - Load conversation │
                    │  - Build prompt      │
                    │  - Route to model    │
                    │  - Stream response   │
                    │  - Save to history   │
                    └─────┬──────────┬────┘
                          │          │
               ┌──────────▼──┐  ┌────▼─────────────┐
               │  Model Router│  │ Conversation DB   │
               │  (LiteLLM)  │  │ (PostgreSQL)       │
               └──────┬───────┘  └───────────────────┘
                      │
         ┌────────────┼────────────────┐
         │            │                │
    ┌────▼───┐   ┌────▼───┐       ┌────▼────┐
    │ Claude │   │ OpenAI │       │ Self-   │
    │  API   │   │  API   │       │ hosted  │
    │(Anthropic)│ │        │       │ (vLLM)  │
    └────────┘   └────────┘       └─────────┘
```

### Conversation Management

```sql
CREATE TABLE conversations (
    id           UUID PRIMARY KEY,
    user_id      BIGINT,
    title        VARCHAR(255),      -- auto-generated from first message
    system_prompt TEXT,
    model        VARCHAR(50),
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY,
    conversation_id UUID REFERENCES conversations(id),
    role            ENUM('system', 'user', 'assistant'),
    content         TEXT,
    input_tokens    INT,            -- for billing
    output_tokens   INT,            -- for billing
    model           VARCHAR(50),    -- which model generated this
    created_at      TIMESTAMP
);

CREATE INDEX idx_conv_messages ON messages(conversation_id, created_at);
```

### Context Window Management

```python
def build_prompt(conversation_id: str, new_user_message: str) -> list[dict]:
    """
    Build messages array for API call, respecting context window limit.
    Strategy: always include system prompt + last N messages that fit.
    """
    system_prompt = get_system_prompt(conversation_id)
    all_messages = get_messages(conversation_id)  # newest first from DB
    
    max_context_tokens = 8000  # leave room for response
    system_tokens = count_tokens(system_prompt)
    new_msg_tokens = count_tokens(new_user_message)
    budget = max_context_tokens - system_tokens - new_msg_tokens
    
    # Include messages from most recent, until budget runs out
    included = []
    tokens_used = 0
    for msg in reversed(all_messages):  # newest first
        msg_tokens = count_tokens(msg['content'])
        if tokens_used + msg_tokens > budget:
            break
        included.append(msg)
        tokens_used += msg_tokens
    
    included.reverse()  # restore chronological order
    
    return [
        {"role": "system", "content": system_prompt},
        *included,
        {"role": "user", "content": new_user_message}
    ]

# When conversation gets too long: summarise old messages
# "Summarise this conversation in 200 words to preserve context"
# Store summary as a special 'summary' message, drop older messages
```

### Streaming Response Flow

```python
async def chat(conversation_id: str, user_message: str):
    # 1. Build prompt (context window management)
    messages = build_prompt(conversation_id, user_message)
    
    # 2. Route to appropriate model
    model = route_to_model(user_message, conversation_id)
    
    # 3. Save user message to DB
    save_message(conversation_id, "user", user_message)
    
    # 4. Stream from LLM
    full_response = ""
    input_tokens = 0
    output_tokens = 0
    
    async for chunk in llm_client.stream(model=model, messages=messages):
        token = chunk.delta.content
        full_response += token
        output_tokens += 1
        yield f"data: {json.dumps({'token': token})}\n\n"  # SSE
    
    # 5. Save assistant response + usage
    save_message(
        conversation_id, "assistant", full_response,
        input_tokens=input_tokens, output_tokens=output_tokens, model=model
    )
    
    # 6. Emit usage event for billing
    kafka.publish("usage.events", {
        "userId": get_user_id(conversation_id),
        "model": model,
        "inputTokens": input_tokens,
        "outputTokens": output_tokens,
        "timestamp": now()
    })
    
    yield "data: [DONE]\n\n"
```

---

## 10. LLM Serving Infrastructure Options

```
Managed APIs (no infrastructure):
  OpenAI API, Anthropic API, Google Gemini API
  Pros: zero ops, latest models, scales automatically
  Cons: cost at scale, data privacy, vendor dependency, rate limits

Self-hosted (full control):
  vLLM: production-grade serving, continuous batching, PagedAttention
    Best throughput, widely adopted, excellent docs
  TGI (Text Generation Inference — HuggingFace): similar to vLLM
  Ollama: easiest to run locally (dev/testing), not production-scale
  TensorRT-LLM (NVIDIA): best raw performance on NVIDIA hardware
  
  Pros: cost at scale (~3-10× cheaper than API for high volume), data privacy
  Cons: GPU ops expertise required, model updates manual

Serverless GPU:
  Modal, RunPod, Replicate, AWS Bedrock (serverless inference)
  Pay per second of GPU time
  Pros: no cold start once warm, scales to zero
  Cons: cold start 10-30s, less control than dedicated

Decision framework:
  < 100K tokens/day:   Use API (cost < $50/month, not worth ops overhead)
  100K-10M tokens/day: API or serverless GPU (compare cost carefully)
  > 10M tokens/day:    Self-hosted vLLM on dedicated GPUs (3-10× cost savings)
```

---

## Interview Q&A

**Q: What is TTFT and why does it matter more than total latency for chat?**
A: TTFT (Time To First Token) is how long the user waits before seeing any output. For streaming responses, a 500ms TTFT feels responsive even if the full response takes 5 seconds — the user sees text appearing immediately and knows the system is working. Total latency matters for non-streaming use cases (batch processing, API integrations). For user-facing chat: optimise TTFT first (faster prefill, smaller models, prompt caching), then overall throughput.

**Q: How do you handle a spike in traffic that exceeds your GPU capacity?**
A: Queue incoming requests (Redis or Kafka queue) with a bounded queue size. Reject with 429 Too Many Requests if queue is full (fail fast is better than indefinite waiting). Auto-scale GPU instances (Kubernetes with GPU node pools, or cloud spot instances) — but GPU cold start is 2-5 minutes (slow). Fallback routing: if primary model is overloaded, route to a cheaper/faster smaller model. Rate limiting per user prevents any one user from consuming all capacity.

**Q: How would you reduce the cost of an LLM feature by 80%?**
A: Multi-pronged approach: (1) Prompt caching — static system prompt cached = 50% discount on input tokens (Anthropic/OpenAI both support this). (2) Model routing — use GPT-4o-mini/Haiku for simple queries, only route complex to expensive models (typically 70% of queries are "simple"). (3) Semantic cache — cache responses to similar queries (20-40% hit rate for typical chatbot). (4) Reduce output tokens — be precise in instructions ("respond in 2 sentences"). (5) Batch non-real-time requests — OpenAI Batch API = 50% discount. Combined: 75-85% cost reduction is achievable.

**Q: What is PagedAttention (vLLM) and why does it matter?**
A: Standard KV cache allocates a contiguous GPU memory block per request at maximum sequence length — wasteful, limits parallelism. PagedAttention (from the vLLM paper) manages KV cache like OS virtual memory: split into small pages, allocate on demand, share pages between requests with the same prefix (e.g., the same system prompt). Result: 2-4× higher throughput and better GPU memory utilisation. It's the key innovation that made vLLM the standard for open-source LLM serving.

**Q: How would you design an LLM system that needs to call external tools (agentic)?**
A: Function calling pattern: define tools as JSON schemas. LLM outputs a structured tool_call when it decides a tool is needed. Your orchestration layer executes the tool, returns the result to the LLM as a tool_result message. The LLM then continues generation with the tool output as context. For multi-step agentic tasks: use an agent loop (LLM → tool call → execute → LLM → tool call → ... → final response). Critical design: set max_iterations (prevent infinite loops), timeout each tool call, and validate tool outputs before feeding back to LLM (guard against prompt injection via malicious tool results).
