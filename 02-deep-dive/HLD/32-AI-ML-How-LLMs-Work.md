# 📚 AI/ML System Design — How Large Language Models Work

> This file covers the **fundamentals** every engineer should know before designing AI-powered systems. You don't need to be an ML researcher — but you need to understand what an LLM is, how it processes input, what its limitations are, and what tradeoffs you're making when you use one.

---

## 1. What is an LLM?

A **Large Language Model** is a neural network trained to predict the next token in a sequence of text. That's it. Everything else — code generation, reasoning, summarisation, translation — emerges from doing this one thing at massive scale.

```
Input:  "The capital of France is"
Model:  predicts next token → "Paris" (highest probability)

Input:  "def factorial(n):"
Model:  predicts → "\n    if n == 0:"
        predicts → "\n        return 1"
        predicts → "\n    return n * factorial(n-1)"
```

The model doesn't "know" facts the way a database does. It has learned **statistical patterns** across trillions of tokens of text. This is why hallucinations happen — when the model predicts a plausible-sounding token sequence that isn't factually true.

---

## 2. Tokens — The Unit of Everything

```
Token ≈ 4 characters ≈ ¾ of a word (English)
"Hello, how are you?" = 5 tokens
A 1,000-word article ≈ 1,300 tokens
This entire 200-page book ≈ ~100,000 tokens

Why tokens matter for system design:
  - Cost: you pay per token (input + output separately)
  - Latency: more tokens = slower generation
  - Context window: max tokens the model can "see" at once
  - Memory: transformer memory scales O(n²) with sequence length
```

### Tokenisation
```
Text → Tokeniser → Integer IDs → Model

"unhappy" → [un, happy] → [1234, 5678]
"tokenization" → [token, ization] → [9012, 3456]

BPE (Byte Pair Encoding): standard algorithm
  Start with characters, merge frequent pairs into subword units
  Vocabulary size: ~50K-100K tokens

Why not word-level tokens?
  "unhappiness", "unhappy", "happiness" share no representation
  Subword tokens capture morphology: "un-" prefix, "-ness" suffix

Why not character-level tokens?
  "hello" = 5 tokens (too many; context window fills fast)
```

---

## 3. Transformer Architecture — What Happens Inside

The Transformer (Vaswani et al., 2017) is the architecture underlying every major LLM.

### High-Level View
```
Input tokens
    │
    ▼
[Embedding Layer]         → tokens → dense vectors (e.g., 4096-dim)
    │
    ▼
[N × Transformer Blocks]  → each block: Attention + Feed-Forward
    │
    ▼
[Output Layer]            → logits over vocabulary (~50K values)
    │
    ▼
[Softmax + Sampling]      → pick next token
```

### Self-Attention — The Key Mechanism
```
Purpose: Let every token "attend" to every other token in context

For each token, compute 3 vectors:
  Q (Query):  "What am I looking for?"
  K (Key):    "What do I contain?"
  V (Value):  "What information do I provide?"

Attention(Q, K, V) = softmax(QKᵀ / √d) × V

Intuition:
  "The bank was robbed" — does "bank" mean river or financial?
  Token "bank" attends to "robbed" → financial context wins
  Token "bank" in "river bank was flooded" → attends to "river" → geographic context

Multi-head attention:
  Run attention H times in parallel (each head learns different patterns)
  H=32 heads for GPT-3; each head specialises (syntax, coreference, facts)
  Concatenate outputs → linear projection

Complexity: O(n²) for sequence length n
  This is why context windows have limits:
  n=4K:  cheap
  n=128K: 1024x more compute than n=4K — expensive
```

### Feed-Forward Network
```
After attention: each token independently processed by a 2-layer MLP
  Learns: factual knowledge, transformations ("Paris is in France")
  Size: 4× the model dimension (e.g., 16384 neurons for d=4096)
  Uses: majority of model parameters (~2/3 of total)
```

### Layer Normalization + Residual Connections
```
Every sub-layer has:
  x → LayerNorm(x) → SubLayer → x + output (residual)

Why residual connections?
  Gradient flows directly to early layers during training (avoids vanishing gradient)
  Model can choose to "skip" a layer if it's not useful
  Critical for training stability at scale
```

---

## 4. Training vs Inference

```
Training:                           Inference:
─────────────────────────────       ─────────────────────────────
Runs ONCE (months of compute)       Runs BILLIONS of times
Gradient descent + backprop         Forward pass only (no gradient)
Batch processing (many examples)    One request at a time (or small batch)
GPU cluster (thousands of GPUs)     GPU server (1-8 GPUs for most models)
Goal: update model weights          Goal: generate tokens fast
Costly: GPT-4 training ~$100M       Costly: $0.01-$0.15 per 1K tokens

Training phases:
  1. Pre-training: predict next token on internet-scale text (self-supervised)
     → Model learns language, facts, reasoning patterns
     → Weeks/months on 1000s of A100s

  2. Instruction Fine-tuning (SFT): train on (instruction, good response) pairs
     → Model learns to follow instructions (not just predict text)
     → Days on 100s of GPUs

  3. RLHF / DPO: align with human preferences
     Reinforcement Learning from Human Feedback:
       Humans rank model outputs (A > B > C)
       Train a reward model on these rankings
       RL to maximise reward → outputs humans prefer
     Direct Preference Optimisation (newer, simpler alternative to RLHF)
     → Days/weeks
```

---

## 5. The Context Window — Critical for System Design

```
Context window = maximum tokens the model can process in one call
                 Input tokens + Output tokens together

Model          Context Window
──────────     ──────────────
GPT-3.5        16K tokens
GPT-4          128K tokens
Claude 3        200K tokens
Gemini 1.5 Pro  1M tokens

What fits in context:
  16K  ≈ 12,000 words ≈ a short novel chapter
  128K ≈ 96,000 words ≈ a full novel
  1M   ≈ 750,000 words ≈ 10 novels

Why context window matters for system design:
  Long document → chunk it (can't fit whole thing)
  Long conversation → truncate or summarise old messages
  Big codebase → can't pass all files at once → need retrieval
  Cost scales with input tokens → long contexts = expensive

KV Cache (Key-Value Cache):
  During inference, attention Q×K computations are CACHED
  This means extending an existing conversation is cheaper than starting fresh
  (only compute attention for NEW tokens, reuse cached KV for old ones)
  Critical for performance: filling cache upfront with system prompt saves compute
```

---

## 6. Generation Parameters — What You Control

```
Temperature (0.0 – 2.0):
  Controls randomness of token sampling
  0.0 = always pick highest probability token (deterministic, repetitive)
  0.7 = balanced (typical default for chat)
  1.5 = very creative/random (sometimes incoherent)
  
  Use cases:
    Code generation: 0.1–0.3 (need correctness, not creativity)
    Creative writing: 0.8–1.2 (want variety)
    Factual Q&A: 0.0–0.3 (want accurate answers)

Top-p (nucleus sampling, 0.0 – 1.0):
  Sample from smallest set of tokens whose cumulative probability ≥ p
  top_p=0.9: only consider tokens representing top 90% of probability mass
  Prevents sampling extremely unlikely tokens (avoids incoherence)
  Works with temperature: usually set one or the other, not both high

Max tokens:
  Hard limit on output length
  Controls cost (output tokens are expensive) and response time
  Set appropriately: 256 for short answers, 4096 for long code

Stop sequences:
  Stop generation when these token sequences appear
  e.g., stop=["\n\nHuman:", "```"] — stops at end of code block
  Use to control output format

System prompt:
  Instructions given to the model before the user's message
  Defines persona, constraints, output format
  Cached via prompt caching (Anthropic, OpenAI) — reduces cost for repeated prefix
```

---

## 7. Why LLMs Hallucinate and What to Do About It

```
Root cause:
  LLMs predict tokens based on statistical patterns, not truth verification
  The model doesn't "check" facts — it predicts what text would follow
  A plausible-sounding but wrong answer scores well during token prediction

Types of hallucinations:
  Factual: "The Eiffel Tower was built in 1887" (wrong year: 1889)
  Attribution: "Einstein said X" (Einstein never said X)
  Fabrication: Made-up citations, fake papers, invented APIs

Mitigation strategies:
  1. RAG (Retrieval Augmented Generation):
     Ground the model with retrieved facts before generating
     "Answer using only the provided context. If not in context, say so."
     → Reduces hallucination for knowledge-retrieval tasks

  2. Structured output + validation:
     Ask for JSON → validate schema → if malformed, retry
     Use function calling → model outputs structured data only

  3. Chain-of-thought prompting:
     "Think step by step before answering"
     Externalises reasoning → easier to verify + fewer errors
     
  4. Self-consistency:
     Generate N responses → take majority answer
     Expensive (N× token cost) but more accurate for complex reasoning
     
  5. Fine-tuning on domain data:
     Fine-tune on your specific domain → less likely to fabricate in that domain
     
  6. Citation requirements:
     "For each claim, cite the source passage"
     Allows post-hoc verification; model is less likely to fabricate when citing

  7. Human-in-the-loop:
     For high-stakes outputs → always require human review
     AI drafts, human approves
```

---

## 8. Model Sizes and Tradeoffs

```
Parameter count → capability vs cost tradeoff:

Model size   Params    Memory needed   Typical use
──────────── ──────    ─────────────   ────────────
Small        7B        ~14GB (FP16)    Edge/mobile, simple tasks
Medium       13-30B    ~26-60GB        API, moderate complexity
Large        70B       ~140GB          High-quality API calls
Very Large   175B+     ~350GB+         GPT-3/4 class, frontier tasks

Memory calculation:
  Parameters × bytes per parameter
  FP32 (32-bit): 1B params × 4 bytes = 4GB
  FP16 (16-bit): 1B params × 2 bytes = 2GB  ← standard inference
  INT8 (8-bit):  1B params × 1 byte  = 1GB  ← quantised
  INT4 (4-bit):  1B params × 0.5B    = 0.5GB ← heavily quantised

Quantisation:
  Reduce precision from FP16 → INT8 or INT4
  Result: 2-4× memory reduction, 1.5-2× speed increase
  Accuracy loss: minimal for INT8; noticeable for INT4 (depends on task)
  
  GPTQ, AWQ: post-training quantisation algorithms
  llama.cpp: runs quantised models on CPU (4-bit quantised 7B = 4GB RAM)

When to use which size:
  Classification/intent detection: 7B fine-tuned > 70B zero-shot (cheaper + faster)
  Complex reasoning, code: need 70B+ (or frontier API)
  RAG + retrieval: smaller models work if retrieval is good
  Creative writing: medium models often sufficient
```

---

## 9. LLM Limitations — What to Design Around

```
1. Context window limit
   → Use RAG, chunking, summarisation for long documents
   → Rolling window for long conversations

2. Hallucination
   → Ground with retrieval, validate outputs, require citations

3. Stale knowledge (training cutoff)
   → Connect to tools: web search, databases, APIs
   → RAG for up-to-date information

4. Cannot perform real actions natively
   → Function calling / tool use (model outputs a structured tool call)
   → Agentic orchestration layer executes the action

5. Expensive at scale
   → Cache common responses (prompt caching, semantic cache)
   → Route simple queries to smaller/cheaper models
   → Batch non-urgent requests

6. Latency: ~500ms-5s for typical responses
   → Streaming (show tokens as generated)
   → Latency-critical tasks: use smaller models or cached responses

7. No persistent memory across conversations (by default)
   → Implement external memory: store conversation summaries in DB
   → Retrieve relevant past context and inject into prompt

8. Non-deterministic
   → Set temperature=0 for reproducible outputs
   → Still not guaranteed identical (floating point on different hardware)
   → Design idempotent pipelines regardless
```

---

## 10. Key Terms Reference

| Term | Meaning |
|------|---------|
| **Token** | Basic unit of text (~4 chars). Cost and latency unit. |
| **Context window** | Max tokens model can process at once |
| **Temperature** | Randomness of generation (0=deterministic, 1=creative) |
| **Hallucination** | Model generates plausible but factually wrong text |
| **Fine-tuning** | Continue training on domain-specific data |
| **RAG** | Retrieve relevant docs, inject into prompt, generate grounded response |
| **Embedding** | Dense vector representation of text (for semantic similarity) |
| **Quantisation** | Reduce model precision (FP16 → INT8) for speed/memory savings |
| **Prompt caching** | Cache KV computation for repeated system prompts |
| **Function calling** | Model outputs structured JSON to invoke a tool/API |
| **Chain of Thought** | "Think step by step" — improves reasoning accuracy |
| **RLHF** | Reinforcement Learning from Human Feedback — alignment technique |
| **SFT** | Supervised Fine-Tuning — teach model to follow instructions |
| **Transformer** | Neural architecture underlying all modern LLMs |
| **Attention** | Mechanism allowing every token to look at every other token |
| **KV Cache** | Cached key-value computation — makes long conversations cheaper |
| **Inference** | Running the model to generate output (vs training = updating weights) |
| **Perplexity** | Measure of model uncertainty — lower = more confident predictions |
