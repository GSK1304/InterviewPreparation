# 📚 DSA Deep Dive — String Algorithms

---

## 🧠 Topics Covered
1. KMP — Pattern Matching O(n+m)
2. Rabin-Karp — Rolling Hash O(n+m) avg
3. Manacher's Algorithm — Longest Palindromic Substring O(n)
4. Z-Algorithm — Pattern Matching O(n+m)
5. String Hashing
6. Suffix Arrays (concept)

---

## 1. KMP — Knuth-Morris-Pratt

**Problem**: Find all occurrences of pattern `p` in text `t`.
**Time**: O(n + m), **Space**: O(m)
**Key idea**: Use a failure function (LPS array) to avoid re-checking characters.

### Build LPS (Longest Proper Prefix which is also Suffix)
```java
int[] buildLPS(String pattern) {
    int m = pattern.length();
    int[] lps = new int[m];
    int len = 0, i = 1;
    while (i < m) {
        if (pattern.charAt(i) == pattern.charAt(len)) {
            lps[i++] = ++len;
        } else if (len != 0) {
            len = lps[len - 1]; // fall back — don't increment i
        } else {
            lps[i++] = 0;
        }
    }
    return lps;
}
```

### KMP Search
```java
List<Integer> kmpSearch(String text, String pattern) {
    int n = text.length(), m = pattern.length();
    int[] lps = buildLPS(pattern);
    List<Integer> matches = new ArrayList<>();
    int i = 0, j = 0;
    while (i < n) {
        if (text.charAt(i) == pattern.charAt(j)) { i++; j++; }
        if (j == m) {
            matches.add(i - j); // match found at index i-j
            j = lps[j - 1];    // look for next match
        } else if (i < n && text.charAt(i) != pattern.charAt(j)) {
            if (j != 0) j = lps[j - 1]; // use LPS to skip
            else i++;
        }
    }
    return matches;
}
```

### Example
```
text    = "AABAACAADAABAABA"
pattern = "AABA"
LPS     = [0, 1, 0, 1]
Matches at indices: 0, 9, 12
```

---

## 2. Rabin-Karp — Rolling Hash

**Problem**: Pattern matching using hashing — excellent for **multiple pattern** search.
**Time**: O(n + m) average, O(nm) worst case
**Key idea**: Compute hash of pattern, slide hash window over text.

```java
List<Integer> rabinKarp(String text, String pattern) {
    int n = text.length(), m = pattern.length();
    long BASE = 31, MOD = 1_000_000_007L;
    List<Integer> matches = new ArrayList<>();

    // Compute hash of pattern and first window
    long patHash = 0, winHash = 0, power = 1;
    for (int i = 0; i < m; i++) {
        patHash = (patHash * BASE + (pattern.charAt(i) - 'a' + 1)) % MOD;
        winHash = (winHash * BASE + (text.charAt(i) - 'a' + 1)) % MOD;
        if (i > 0) power = power * BASE % MOD;
    }

    for (int i = 0; i <= n - m; i++) {
        if (winHash == patHash) {
            // Verify to handle hash collisions
            if (text.substring(i, i + m).equals(pattern))
                matches.add(i);
        }
        // Roll the hash window
        if (i < n - m) {
            winHash = (winHash - (text.charAt(i) - 'a' + 1) * power % MOD + MOD) % MOD;
            winHash = (winHash * BASE + (text.charAt(i + m) - 'a' + 1)) % MOD;
        }
    }
    return matches;
}
```

### Multiple Pattern Search (Rabin-Karp shines here)
```java
// Search for any of K patterns simultaneously — O(n + sum of pattern lengths)
Set<Long> patternHashes = new HashSet<>();
for (String p : patterns) patternHashes.add(computeHash(p));
// Slide window of each pattern length and check hash membership
```

---

## 3. Manacher's Algorithm — Longest Palindromic Substring O(n)

**Problem**: Find longest palindromic substring in O(n).
**Key idea**: Use previously computed palindrome radii to avoid redundant checks.

```java
String manacher(String s) {
    // Transform: "abc" → "#a#b#c#" (handles even/odd uniformly)
    String t = "#" + String.join("#", s.split("")) + "#";
    int n = t.length();
    int[] p = new int[n]; // p[i] = radius of palindrome centered at i
    int center = 0, right = 0;

    for (int i = 0; i < n; i++) {
        if (i < right) p[i] = Math.min(right - i, p[2 * center - i]); // mirror
        // Expand around i
        while (i - p[i] - 1 >= 0 && i + p[i] + 1 < n
               && t.charAt(i - p[i] - 1) == t.charAt(i + p[i] + 1))
            p[i]++;
        // Update center and right boundary
        if (i + p[i] > right) { center = i; right = i + p[i]; }
    }

    // Find maximum radius
    int maxLen = 0, centerIdx = 0;
    for (int i = 0; i < n; i++) if (p[i] > maxLen) { maxLen = p[i]; centerIdx = i; }

    // Map back to original string
    int start = (centerIdx - maxLen) / 2;
    return s.substring(start, start + maxLen);
}
```

### Example
```
s = "babad"
t = "#b#a#b#a#d#"
p = [0,1,0,3,0,3,0,1,0,1,0]
Longest palindrome: "bab" or "aba" (length 3)
```

---

## 4. Z-Algorithm

**Problem**: For string `s`, compute Z[i] = length of longest substring starting at `s[i]` that is also a prefix of `s`.
**Time**: O(n), great for pattern matching.

```java
int[] zFunction(String s) {
    int n = s.length();
    int[] z = new int[n];
    z[0] = n;
    int l = 0, r = 0;
    for (int i = 1; i < n; i++) {
        if (i < r) z[i] = Math.min(r - i, z[i - l]);
        while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
        if (i + z[i] > r) { l = i; r = i + z[i]; }
    }
    return z;
}

// Pattern matching using Z-algorithm
List<Integer> zSearch(String text, String pattern) {
    String combined = pattern + "$" + text; // $ = separator not in alphabet
    int[] z = zFunction(combined);
    List<Integer> matches = new ArrayList<>();
    int m = pattern.length();
    for (int i = m + 1; i < combined.length(); i++)
        if (z[i] == m) matches.add(i - m - 1);
    return matches;
}
```

---

## 5. String Hashing

**Problem**: Compare substrings in O(1) after O(n) preprocessing.

```java
class StringHash {
    long[] hash, power;
    long BASE = 131, MOD = 1_000_000_007L;

    StringHash(String s) {
        int n = s.length();
        hash = new long[n + 1];
        power = new long[n + 1];
        power[0] = 1;
        for (int i = 0; i < n; i++) {
            hash[i + 1] = (hash[i] * BASE + s.charAt(i)) % MOD;
            power[i + 1] = power[i] * BASE % MOD;
        }
    }

    // Get hash of s[l..r] (0-indexed, inclusive) in O(1)
    long getHash(int l, int r) {
        return (hash[r + 1] - hash[l] * power[r - l + 1] % MOD + MOD) % MOD;
    }

    // Compare s[l1..r1] == s[l2..r2] in O(1)
    boolean equals(int l1, int r1, int l2, int r2) {
        return getHash(l1, r1) == getHash(l2, r2);
    }
}
```

---

## 6. Key String Interview Patterns

```java
// Anagram check — O(n)
boolean isAnagram(String s, String t) {
    if (s.length() != t.length()) return false;
    int[] count = new int[26];
    for (char c : s.toCharArray()) count[c - 'a']++;
    for (char c : t.toCharArray()) if (--count[c - 'a'] < 0) return false;
    return true;
}

// Longest common prefix
String longestCommonPrefix(String[] strs) {
    if (strs.length == 0) return "";
    String prefix = strs[0];
    for (String s : strs)
        while (!s.startsWith(prefix)) prefix = prefix.substring(0, prefix.length() - 1);
    return prefix;
}

// Check rotation: s2 is rotation of s1 iff s2 is substring of s1+s1
boolean isRotation(String s1, String s2) {
    return s1.length() == s2.length() && (s1 + s1).contains(s2);
}

// Count distinct substrings using hashing — O(n²) with O(1) compare
int countDistinctSubstrings(String s) {
    Set<Long> seen = new HashSet<>();
    StringHash sh = new StringHash(s);
    for (int i = 0; i < s.length(); i++)
        for (int j = i; j < s.length(); j++)
            seen.add(sh.getHash(i, j));
    return seen.size();
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Trade Message Pattern Detection (KMP — kACE)
**Problem**: Search for specific FX message patterns (e.g., "REJECT", "TIMEOUT") in high-volume trade message streams.

```java
// KMP for exact pattern matching in trade messages — O(n+m) per message
List<Integer> rejectPositions = kmpSearch(tradeMessageStream, "REJECT");
List<Integer> timeouts = kmpSearch(tradeMessageStream, "TIMEOUT");
// Far faster than regex for known fixed patterns
```
**Where it applies**: Kafka consumer message filtering, WebSocket STOMP frame parsing in kACE.
> 🏭 **Industry Example**: Elasticsearch uses KMP-variant for exact phrase matching in full-text search. Antivirus software (ClamAV) uses KMP to scan file streams for malware signatures. Linux `grep` uses optimized string matching (BM/KMP hybrid) for fast text search.
> 🏦 **kACE Context**: kACE Kafka consumer message filtering — fast scanning for "REJECT"/"TIMEOUT" patterns in high-volume trade message streams.


---

### Use Case 2: Duplicate Code Detection (Rabin-Karp — CI/CD)
**Problem**: Find duplicate code blocks across multiple files — multiple pattern search.

```java
// Rabin-Karp: compute hashes of all K-line blocks, find collisions
Map<Long, List<Integer>> blockHashes = new HashMap<>();
for (int i = 0; i <= lines.size() - K; i++) {
    long hash = computeBlockHash(lines, i, K);
    blockHashes.computeIfAbsent(hash, x -> new ArrayList<>()).add(i);
}
// Lines with same hash = potential duplicates → verify
```
**Where it applies**: GitLab CI duplicate code detection, kACE codebase quality checks.
> 🏭 **Industry Example**: GitHub's code search uses rolling hash for duplicate detection. Stack Overflow's plagiarism detection uses Rabin-Karp for finding copied code answers. SonarQube's code duplication detection uses rolling hash to find duplicated code blocks across files.
> 🏦 **kACE Context**: GitLab CI duplicate code detection in the kACE codebase quality checks.


---

### Use Case 3: FX Symbol Palindrome Validation (Manacher's)
**Problem**: Find all palindromic sub-patterns in currency pair sequences for pattern analysis.

```java
// Manacher's O(n) — find all palindromic windows in FX rate sequence
String longestPalinPattern = manacher(fxRateSequence);
// Also: find ALL palindromes of each length efficiently
```
**Where it applies**: FX rate pattern recognition, symmetric option strategy detection.
> 🏭 **Industry Example**: Bioinformatics uses Manacher's to find palindromic DNA sequences for restriction enzyme sites. Competitive programming platforms (Codeforces, LeetCode) feature it as a landmark algorithm problem. Text editors use Manacher's for efficient palindrome highlighting.
> 🏦 **kACE Context**: FX rate pattern recognition — finding symmetric patterns in currency pair price sequences.


---

### Use Case 4: Log Prefix Grouping (Z-Algorithm + String Hashing)
**Problem**: Group log messages by their common prefix efficiently.

```java
// Z-function to find how much of each log shares prefix with template
String template = "ERROR: Connection refused to";
String combined = template + "$" + logMessage;
int[] z = zFunction(combined);
int commonPrefixLen = z[template.length() + 1]; // how much matches
```
**Where it applies**: kACE log aggregation — grouping similar errors, Kibana log pattern detection.
> 🏭 **Industry Example**: Splunk's log clustering algorithm uses Z-function for prefix-based log grouping. Kibana's log pattern analysis uses prefix matching to group similar error messages. Linux `journald` uses prefix matching to deduplicate repeated log lines.
> 🏦 **kACE Context**: kACE log aggregation — grouping similar error messages to reduce alert noise.


---

### Use Case 5: Substring Comparison for Config Diff (String Hashing)
**Problem**: Compare sections of two large config files in O(1) per comparison after O(n) preprocessing.

```java
StringHash h1 = new StringHash(configV1);
StringHash h2 = new StringHash(configV2);
// Binary search + hash to find first differing position in O(n log n)
int lo = 0, hi = Math.min(configV1.length(), configV2.length());
while (lo < hi) {
    int mid = (lo + hi) / 2;
    if (h1.getHash(0, mid) == h2.getHash(0, mid)) lo = mid + 1;
    else hi = mid;
}
int firstDiff = lo; // first position where configs diverge
```
**Where it applies**: kACE layout config versioning, Spring Boot property file diff detection.
> 🏭 **Industry Example**: Git uses SHA-1/SHA-256 content hashing to detect file changes in O(1) — the same principle as string hashing. React's reconciliation uses content hashing to detect prop changes. Docker uses content-addressable storage (SHA-256 hash of layers) for deduplication.
> 🏦 **kACE Context**: kACE layout config versioning — O(1) comparison of config sections using string hashing to detect diffs.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Implement strStr() | KMP / Z-algo | Easy |
| 2 | Repeated Substring Pattern | KMP LPS | Easy |
| 3 | Shortest Palindrome | KMP / Z-algo | Hard |
| 4 | Longest Palindromic Substring | Manacher's / DP | Medium |
| 5 | Palindromic Substrings (count) | Manacher's / expand | Medium |
| 6 | Longest Duplicate Substring | Rabin-Karp + BS | Hard |
| 7 + | Find All Anagrams in String | Sliding window | Medium |
| 8 | Group Anagrams | HashMap + sort | Medium |
| 9 | Minimum Window Substring | Sliding window | Hard |
| 10 | String to Integer (atoi) | Parsing | Medium |

---

## ⚠️ Common Mistakes

- KMP: LPS build — when mismatch with `len > 0`, set `len = lps[len-1]` (don't reset to 0 directly)
- Rabin-Karp: always **verify** after hash match to handle collisions
- Rabin-Karp rolling: subtract old char's contribution using `power[m-1]` (precomputed)
- Manacher's: transform string with `#` separators to handle even-length palindromes uniformly
- String hashing: use `(hash - x + MOD) % MOD` to avoid negative values in Java
- Z-algorithm: use separator `$` (char not in alphabet) between pattern and text to prevent cross-matching
- `String.substring()` in Java is O(n) — use `StringHash` for O(1) comparison in algorithms
