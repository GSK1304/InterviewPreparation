# 📚 AI/ML System Design — RAG & Vector Databases

---

## 1. What is RAG and Why Does It Exist?

**RAG (Retrieval-Augmented Generation)** solves the core limitations of LLMs used for knowledge-intensive tasks:

```
Problem without RAG:
  LLM's knowledge is frozen at training cutoff
  LLM doesn't know your company's internal docs
  LLM hallucinates when asked about specific facts
  Putting all docs in context = too expensive + hits window limit

RAG solution:
  1. RETRIEVE: find the most relevant documents for this query
  2. AUGMENT:  inject those documents into the prompt as context
  3. GENERATE: model answers using the provided context (not memory)

Result:
  ✅ Always up-to-date (index docs as they change)
  ✅ Grounded in your data (cite sources, reduce hallucination)
  ✅ No context limit problem (only relevant chunks go in)
  ✅ Cheaper than long-context (only relevant text, not all documents)
```

### RAG vs Fine-Tuning vs Long Context

| | RAG | Fine-Tuning | Long Context (stuffing) |
|--|-----|------------|------------------------|
| **Updates** | Index new docs any time | Requires re-training | Update the document |
| **Cost** | Medium (retrieval + generation) | High upfront (training) | High per query (long prompt) |
| **Hallucination** | Reduced (grounded) | Depends on training data | Depends on what fits |
| **Latency** | +50-100ms for retrieval | Same as base model | Slow (large context) |
| **Use when** | Knowledge base Q&A, docs search | Domain-specific behaviour | Whole-document analysis |

---

## 2. RAG Architecture — Two Acts

### Act 1: Offline (Indexing Pipeline) — Runs Once Per Document

```
Raw Documents (PDF, Word, HTML, Markdown, Code, Database records)
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  Document Loader                                                 │
│  Parse: PDF→text (pdfplumber), HTML→text (BeautifulSoup),       │
│  DOCX→text (python-docx), code→text (as-is)                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Chunker                                                         │
│  Split document into chunks (overlapping windows)               │
│  Chunk size: 256–1024 tokens (tune for your use case)           │
│  Overlap: 10-20% (context continuity across chunks)             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Embedding Model                                                 │
│  Each chunk → 768 or 1536-dim dense vector                      │
│  text-embedding-ada-002 (OpenAI), BGE (open-source), E5         │
│  CRITICAL: same model used at query time                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Vector Database                                                 │
│  Store: (chunkId, embedding vector, metadata, original text)    │
│  Index: HNSW or IVF for fast ANN (Approximate Nearest Neighbour)│
│  Tools: Pinecone, Weaviate, Qdrant, Chroma, Milvus, pgvector    │
└─────────────────────────────────────────────────────────────────┘
```

### Act 2: Online (Query Time) — Runs on Every Request

```
User Query: "What is our refund policy for digital products?"
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│  Query Embedding                                             │
│  Same embedding model → query vector (1536-dim)             │
│  CRITICAL: same model as indexing — incompatible spaces!    │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  ANN Search (Approximate Nearest Neighbour)                  │
│  Find top-K chunks with highest cosine similarity to query   │
│  K=5 to 20 candidates (tune for quality vs latency)          │
│  Latency: 10-50ms for 1M vectors                            │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  Re-Ranker (optional but often worth it)                     │
│  Cross-encoder re-scores top-K candidates more accurately    │
│  Cohere Rerank, BGE Reranker, or a small BERT cross-encoder  │
│  Reduces from K=20 candidates to K=5 final                  │
│  Latency: +20-80ms but significant quality improvement      │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  Prompt Assembly                                             │
│  System: "Answer using ONLY the provided context..."        │
│  Context: [chunk1 text] [chunk2 text] [chunk3 text]         │
│  User: "What is our refund policy for digital products?"    │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│  LLM Generation                                              │
│  Model reads context → generates grounded answer            │
│  Can cite: "According to the Returns Policy doc..."         │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. Chunking Strategy — Critical Design Decision

```
Too small chunks (64 tokens):
  ✅ Precise retrieval (narrow context window)
  ❌ Loses context — a sentence fragment without surrounding meaning
  ❌ More chunks to store and search
  
Too large chunks (2048 tokens):
  ✅ Rich context per chunk
  ❌ Dilutes relevance — chunk contains many topics, cosine similarity is blurred
  ❌ Fewer relevant characters in returned context
  
Sweet spot: 256–512 tokens for most cases

Chunking strategies:
  Fixed-size: split every N tokens with M overlap
    Simple, works for most cases
    Problem: may split mid-sentence or mid-paragraph
    
  Sentence-level: split on sentence boundaries
    Preserves sentence integrity
    Uneven chunk sizes
    
  Recursive (LangChain default): try to split on \n\n → \n → space → char
    Respects document structure (paragraphs first)
    
  Semantic chunking: embed sentences, split where similarity drops
    Best quality, most expensive
    Splits where topic changes
    
  Document-specific: headers for Markdown, slides for PPTX, functions for code
    Best for structured documents
    
Overlap:
  Chunks 1-2 share their last/first 20% of tokens
  Ensures context that spans chunk boundary is still retrievable
  Typical: 50-100 token overlap
```

---

## 4. Embeddings — What They Are and Why They Matter

```
Embedding = a vector (list of numbers) representing the semantic meaning of text

"I love pizza" → [0.12, -0.34, 0.87, ..., 0.23]  ← 1536 numbers
"I enjoy pizza" → [0.11, -0.33, 0.85, ..., 0.22]  ← similar! high cosine sim
"The stock market crashed" → [-0.45, 0.12, -0.23, ..., 0.67]  ← very different

Cosine similarity:
  similarity(A, B) = (A · B) / (|A| × |B|)
  Range: -1 (opposite) to +1 (identical)
  Threshold for "relevant": typically > 0.7-0.8

Why does this work?
  Training: embeddings trained to map similar-meaning texts close together
  Geometry: relationships are encoded: king - man + woman ≈ queen (word2vec era)
  Dense retrieval: captures semantic similarity that exact keyword match misses
    "cardiac arrest" matches "heart attack" (different words, same meaning)

Embedding model choice:
  text-embedding-ada-002 (OpenAI): 1536-dim, cloud API, $0.0001/1K tokens
  text-embedding-3-small (OpenAI): 1536-dim, cheaper, better quality
  BGE-large-en-v1.5 (open-source): 1024-dim, run locally, excellent quality
  E5-large-v2 (open-source): strong multilingual support
  
  CRITICAL: You cannot mix embeddings from different models
  If you switch models → must re-embed ALL documents (full re-index)

Embedding dimensions and storage:
  768-dim FP32: 768 × 4 bytes = 3KB per chunk
  1M chunks × 3KB = 3GB (fits in memory on a single server)
  10M chunks = 30GB (Redis cluster or dedicated vector DB)
  1B chunks = 3TB (needs distributed vector DB with sharding)
```

---

## 5. Vector Databases

### What a Vector DB Does (vs Regular DB)

```
Regular DB:          WHERE name = 'John'           (exact match, B-Tree index)
Vector DB:           WHERE embedding NEAREST TO query_vector (ANN search, HNSW index)

The ANN Problem:
  Given: 1M vectors of 1536 dimensions
  Query: find 10 vectors most similar to this new vector
  Naive: compute cosine similarity to ALL 1M vectors → O(1M) → 1-2 seconds ❌
  ANN:   approximate answer in O(log N) → 1-50ms ✅ (with minor accuracy loss)
```

### HNSW — The Standard ANN Index

```
HNSW (Hierarchical Navigable Small World):
  Multi-layer graph where:
    Top layers: long-range connections (navigate quickly to region)
    Bottom layers: dense local connections (find nearest neighbours)
  
  Query:
    1. Start at top layer entry point
    2. Greedily traverse to closest node
    3. Move to lower layer, repeat
    4. At bottom layer: found approximate nearest neighbours
    
  Build time: O(n log n)  — slower than IVF but one-time cost
  Query time: O(log n)    — very fast, < 1ms for 1M vectors
  Accuracy:   95-99% recall vs exact search (tunable: ef parameter)
  Memory:     Stores graph in RAM — 1M vectors × 1536-dim = ~12GB + graph overhead

Used by: Qdrant, Weaviate, Milvus (default index)
```

### Vector DB Comparison

| Database | Type | Strengths | Weaknesses | Use When |
|----------|------|-----------|-----------|---------|
| **Pinecone** | Managed cloud | Simplest to start, scales well | Expensive, vendor lock-in | Startup, don't want to manage infra |
| **Weaviate** | Self-hosted + cloud | Hybrid search built-in, GraphQL | Complex setup | Hybrid search, open-source requirement |
| **Qdrant** | Self-hosted + cloud | Fast, Rust-based, payload filtering | Newer, smaller community | Performance-critical, self-hosted |
| **Chroma** | Self-hosted | Easiest dev experience, Python-native | Limited production scale | Prototyping, small-scale |
| **Milvus** | Self-hosted + cloud | Massive scale (billions), diverse indexes | Complex ops | Billion-scale vector search |
| **pgvector** | PostgreSQL extension | Use existing Postgres, ACID | Slower than dedicated DBs | Small scale, already on Postgres |
| **Redis Vector** | In-memory | Sub-ms latency | Memory-only, expensive | Real-time, latency-critical |
| **FAISS** | Library (not DB) | Fastest raw performance, Facebook | No persistence, no API | Research, custom systems |

---

## 6. Hybrid Search — Dense + Sparse

```
Problem with pure vector (dense) search:
  Query: "Python async def syntax"
  Vector search might return: general Python docs (semantically similar)
  But misses: exact syntax documentation (exact keyword match would find this)

Sparse retrieval (BM25/keyword search):
  Traditional TF-IDF / BM25 scoring
  Great for exact keyword matches
  Misses semantic similarity (won't match "cardiac arrest" → "heart attack")

Hybrid search = dense + sparse, combined:

  dense_score  = cosine_similarity(query_embedding, chunk_embedding)
  sparse_score = BM25(query_terms, chunk_text)
  
  hybrid_score = α × dense_score + (1-α) × sparse_score
  α = 0.7 (tune based on your queries)

Reciprocal Rank Fusion (RRF) — alternative combination:
  rank_dense  = rank of chunk in dense results
  rank_sparse = rank of chunk in sparse results
  rrf_score   = 1/(k + rank_dense) + 1/(k + rank_sparse)  [k=60 typical]
  
  Advantage: no need to calibrate scores from different systems

Weaviate, Elasticsearch, and Qdrant all support hybrid search natively.
For most production RAG systems: hybrid > pure vector.
```

---

## 7. Re-Ranking — Closing the Accuracy Gap

```
Vector search is fast but approximate. Re-ranking is slower but more accurate.

Two-stage pipeline:
  Stage 1: Vector search retrieves top-K candidates (K=20-50, fast)
  Stage 2: Cross-encoder re-ranks those K candidates (slower but precise)
  Return: top-N final results (N=3-10)

Cross-encoder vs bi-encoder:

  Bi-encoder (what vector search uses):
    Embed query separately, embed document separately
    Score = cosine similarity of two embeddings
    Fast: embed once, compare many
    
  Cross-encoder (what re-ranking uses):
    Input: [query + document] concatenated → single forward pass
    Output: relevance score 0-1
    More accurate: sees both together, can model interactions
    Slower: one forward pass per (query, document) pair
    
  Cohere Rerank API: managed cross-encoder re-ranking service
  BGE-reranker: open-source cross-encoder
  
Latency budget:
  Vector search: 10-20ms
  Re-ranking (20 candidates): +20-80ms
  LLM generation: 500ms-2s
  Total: 600ms-2s typical RAG pipeline
```

---

## 8. Advanced RAG Patterns

### Query Rewriting
```
Problem: User asks ambiguous question → vector search returns poor results

Solution: Use LLM to rewrite query before embedding
  User: "what did they say about the deadline"
  Rewritten: "project deadline policy and communication in the meeting notes"
  
Or HyDE (Hypothetical Document Embedding):
  LLM generates a hypothetical "perfect answer document"
  Embed the hypothetical document → search for similar real documents
  Often finds better matches than embedding the query directly
  
  Query: "What is the capital of Australia?"
  HyDE: "The capital of Australia is Canberra. Canberra was chosen as..."
  Search: chunks about Canberra found (not Sydney, despite Sydney being mentioned more)
```

### Multi-Query Retrieval
```
Generate N variants of the original question → retrieve for each → merge + deduplicate
  Original: "How does RAG work?"
  Variant 1: "What is retrieval augmented generation?"
  Variant 2: "How do you add external knowledge to an LLM?"
  Variant 3: "What is the architecture of a RAG system?"
  
Retrieve 5 docs for each → merge → deduplicate → re-rank → return top 5
Result: higher recall than single-query retrieval
```

### Contextual Retrieval (Anthropic)
```
Problem: chunks lose context after being extracted from their document
  "The meeting was rescheduled to Tuesday" → which meeting?

Solution: Before indexing, use LLM to prepend context to each chunk
  Original chunk: "The meeting was rescheduled to Tuesday"
  Contextualised: "This chunk is from the Q3 Planning Document for Project Phoenix.
                   The meeting was rescheduled to Tuesday"
  
Embed the contextualised chunk → much better retrieval accuracy
Cost: ~$5/M tokens to contextualise (one-time cost at indexing)
Anthropic reported 49% reduction in retrieval failures with this approach
```

### Parent Document Retrieval
```
Index small chunks → retrieve → return PARENT document

Why:
  Small chunk (128 tokens): precise retrieval
  But: the answer may span a larger section (512 tokens)
  
  Index: small chunks (128 tokens) → fine-grained search
  Retrieve: find the most relevant small chunk
  Return: the PARENT section (512 tokens) containing that chunk
  
LangChain has built-in support for this pattern.
```

---

## 9. RAG Evaluation Metrics

```
Retrieval quality:
  Recall@K:     % of relevant docs found in top-K results (want: > 80%)
  Precision@K:  % of top-K results that are relevant (want: > 60%)
  MRR:          Mean Reciprocal Rank (is the best answer at position 1?)

Generation quality:
  Faithfulness:     does the answer contradict the retrieved context? (hallucination check)
  Answer relevance: does the answer address the question?
  Context relevance: is the retrieved context relevant to the question?

Frameworks:
  RAGAS: automated RAG evaluation library (uses LLM as judge)
  TruLens: observability for LLM apps
  LangSmith: LangChain's evaluation + tracing platform
  
  "LLM as judge": use a powerful LLM to score responses
    Human correlation: ~80-90% agreement with human evaluators
    Much cheaper than human evaluation at scale
```

---

## 10. Production RAG Architecture

```
                        User Query
                             │
                    ┌────────▼────────┐
                    │   Query Service  │
                    │  - Query rewrite │
                    │  - Cache check   │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────────────────────────┐
                    │         Retrieval Service             │
                    │  Dense: Vector DB (Qdrant/Pinecone)  │
                    │  Sparse: Elasticsearch (BM25)        │
                    │  Merge: RRF or weighted blend        │
                    └────────┬─────────────────────────────┘
                             │ top-20 candidates
                    ┌────────▼────────┐
                    │  Re-Ranker      │  top-5 final
                    │  (Cohere/BGE)   │──────────────────┐
                    └─────────────────┘                  │
                                                         │
                    ┌────────────────────────────────────▼┐
                    │         LLM Service                  │
                    │  Prompt: system + context + query    │
                    │  Model: Claude/GPT-4/Llama          │
                    │  Streaming: yes                     │
                    └────────────────────────────────────┘

Supporting services:
  Document Ingestion: Kafka → chunker → embedder → vector DB
  Cache: Redis semantic cache (similar queries → same cached answer)
  Observability: LangSmith / LangFuse (trace every retrieval + generation)
  Feedback: thumbs up/down → labeled data for improving retrieval
```

---

## Interview Q&A

**Q: When would you choose RAG over fine-tuning?**
A: RAG when: knowledge changes frequently (use today's docs), you need citations/source attribution, you have a large knowledge base, or you want to add knowledge without expensive retraining. Fine-tuning when: you need the model to behave differently (style, format, domain-specific tone), the task requires specialised reasoning not in base model, or latency doesn't allow a retrieval step. Most production systems use RAG for knowledge and fine-tuning for behaviour — they're complementary.

**Q: What happens if the embedding model changes?**
A: Catastrophic — you cannot mix embeddings from different models. Vectors from model A live in a completely different geometric space than model B. If you change models, you must re-embed ALL documents and rebuild the entire index. This is why embedding model versioning is critical: pin to a specific model version, have a re-indexing pipeline ready, blue-green swap the vector index (build new index in background, switch atomically).

**Q: How do you handle a document that's too large to fit in context even after chunking?**
A: MapReduce pattern: split into N chunks → process each chunk separately ("summarise this section") → combine summaries → final synthesis. For question answering: retrieve most relevant chunks only (trust retrieval quality). For full document analysis: hierarchical summarisation — summarise each page → summarise the summaries → final answer. Long-context models (Claude 200K) can handle many cases but cost scales with length.

**Q: How would you design a RAG system for code search?**
A: Code requires special treatment: chunk by function/class (not fixed tokens), preserve file path and imports as metadata. Embed at function level. For retrieval: hybrid search (exact function names via BM25 + semantic similarity via embeddings). Add AST-based metadata (function signature, docstring, language). Query rewriting: "how to parse JSON" → "JSON.parse(), json.loads(), JSON deserialisation". Use a code-specific embedding model (CodeBERT, text-embedding-3-small is also strong for code).

**Q: What is the "lost in the middle" problem and how do you address it?**
A: LLMs pay more attention to text at the beginning and end of context — information in the middle is often missed. For RAG: put the most relevant chunk FIRST (not last). For long contexts: use re-ranking to ensure most relevant chunks aren't buried in the middle. Some research uses "reciprocal attention" prompting or query-aware chunking to mitigate this. In practice: keep retrieved context short (3-5 chunks max), ensuring the relevant content isn't buried.
