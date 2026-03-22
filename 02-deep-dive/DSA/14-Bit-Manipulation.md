# 📚 DSA Deep Dive — Bit Manipulation

---

## 🧠 Core Concepts

### Bitwise Operators
| Operator | Symbol | Example | Result |
|----------|--------|---------|--------|
| AND | `&` | `5 & 3` → `101 & 011` | `001` = 1 |
| OR | `\|` | `5 \| 3` → `101 \| 011` | `111` = 7 |
| XOR | `^` | `5 ^ 3` → `101 ^ 011` | `110` = 6 |
| NOT | `~` | `~5` → `~00000101` | `11111010` = -6 |
| Left Shift | `<<` | `5 << 1` | `10` = 10 (×2) |
| Right Shift | `>>` | `5 >> 1` | `2` = 2 (÷2) |
| Unsigned Right | `>>>` | `-1 >>> 1` | `2147483647` |

### Key XOR Properties
```
a ^ 0 = a          (identity)
a ^ a = 0          (self-inverse)
a ^ b = b ^ a      (commutative)
(a ^ b) ^ c = a ^ (b ^ c)  (associative)
```

---

## 🔑 Essential Tricks

### Check / Set / Clear / Toggle a Bit
```java
int n = 13; // binary: 1101
int i = 1;  // bit position (0-indexed from right)

// Check if bit i is set
boolean isSet = (n & (1 << i)) != 0;  // true (bit 1 of 1101 = 0... wait, 1101 bit1=0)

// Set bit i
int set = n | (1 << i);      // 1101 | 0010 = 1111

// Clear bit i
int cleared = n & ~(1 << i); // 1101 & 1101 = 1101 (clears bit i)

// Toggle bit i
int toggled = n ^ (1 << i);  // 1101 ^ 0010 = 1111
```

### Common Patterns
```java
// Is n a power of 2?
boolean isPow2 = n > 0 && (n & (n - 1)) == 0;
// Explanation: power of 2 has exactly one bit set → n-1 flips all lower bits

// Get lowest set bit
int lowest = n & (-n);  // -n = ~n + 1

// Clear lowest set bit
int cleared = n & (n - 1);  // used in counting set bits

// Count set bits (Brian Kernighan)
int countBits(int n) {
    int count = 0;
    while (n != 0) { n &= (n - 1); count++; }
    return count;
}

// Swap two numbers without temp
a ^= b; b ^= a; a ^= b;

// Check if bit i is set (cleaner)
boolean check = ((n >> i) & 1) == 1;

// Multiply/divide by 2
int mul2 = n << 1;
int div2 = n >> 1;

// Get all subsets of a set using bits
for (int mask = 0; mask < (1 << n); mask++) {
    // mask represents a subset — bit i set means element i is included
    List<Integer> subset = new ArrayList<>();
    for (int i = 0; i < n; i++)
        if ((mask & (1 << i)) != 0) subset.add(arr[i]);
}
```

### Single Number (XOR trick)
```java
// Find element appearing once when all others appear twice
int singleNumber(int[] nums) {
    int result = 0;
    for (int n : nums) result ^= n; // pairs cancel out
    return result;
}
```

### Reverse Bits
```java
int reverseBits(int n) {
    int result = 0;
    for (int i = 0; i < 32; i++) {
        result = (result << 1) | (n & 1);
        n >>= 1;
    }
    return result;
}
```

### Bitmask DP Template
```java
// dp[mask] = optimal value for subset represented by mask
int n = items.length;
int[] dp = new int[1 << n]; // 2^n states
for (int mask = 0; mask < (1 << n); mask++) {
    for (int i = 0; i < n; i++) {
        if ((mask & (1 << i)) != 0) { // item i is in this subset
            int prevMask = mask ^ (1 << i); // mask without item i
            dp[mask] = Math.max(dp[mask], dp[prevMask] + value[i]);
        }
    }
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: Feature Flags / Permission Bitmask (kACE Auth)
**Problem**: Store and check user permissions efficiently using a bitmask.

```java
// Each permission is a bit position
static final int READ    = 1 << 0; // 0001
static final int WRITE   = 1 << 1; // 0010
static final int EXECUTE = 1 << 2; // 0100
static final int ADMIN   = 1 << 3; // 1000

int userPermissions = READ | WRITE; // 0011

// Check permission
boolean canWrite = (userPermissions & WRITE) != 0;     // true
boolean isAdmin  = (userPermissions & ADMIN) != 0;     // false

// Grant permission
userPermissions |= EXECUTE;  // add execute

// Revoke permission
userPermissions &= ~WRITE;   // remove write

// Toggle
userPermissions ^= READ;     // toggle read
```
**Where it applies**: kACE JWT privileges bitmask, user role permission system, feature flag toggles.
> 🏭 **Industry Example**: Linux file permissions (rwxr-xr-x) use bitmasks — `chmod 755` = `111 101 101` in binary. Discord stores user permissions per server as a 64-bit bitmask. AWS IAM policy conditions use bitmask-style flags internally. LaunchDarkly's feature flag system uses bitmask evaluation for multi-variate flags.
> 🏦 **kACE Context**: kACE JWT privileges bitmask — READ/WRITE/EXECUTE/ADMIN permissions stored as bit flags per user role.


---

### Use Case 2: Subset Generation for Option Legs
**Problem**: Generate all possible subsets of option legs for strategy analysis.

```java
// n legs → 2^n possible combinations
int n = legs.size();
List<List<OptionLeg>> allCombinations = new ArrayList<>();
for (int mask = 1; mask < (1 << n); mask++) { // skip empty set (mask=0)
    List<OptionLeg> combo = new ArrayList<>();
    for (int i = 0; i < n; i++)
        if ((mask & (1 << i)) != 0) combo.add(legs.get(i));
    if (isValidStrategy(combo)) allCombinations.add(combo);
}
```
**Where it applies**: kACE multi-leg FX option strategy generation (straddle, strangle, butterfly).
> 🏭 **Industry Example**: Bloomberg's portfolio optimizer enumerates all 2^n asset subsets using bitmask iteration. Python's `itertools.combinations` internally uses bitmask enumeration. Google's Knapsack-based ad auction system uses bitmask DP for optimal ad slot assignment.
> 🏦 **kACE Context**: kACE multi-leg FX option strategy generation — enumerating all valid subsets of available legs.


---

### Use Case 3: State Compression — Visited States in Cache
**Problem**: Track which microservices have been initialized using a compact bitmask.

```java
int initialized = 0; // bitmask for 32 services
int AUTH_SERVICE    = 1 << 0;
int PRICING_SERVICE = 1 << 1;
int RFQ_SERVICE     = 1 << 2;
int KAFKA_SERVICE   = 1 << 3;

// Mark service as initialized
initialized |= AUTH_SERVICE;
initialized |= PRICING_SERVICE;

// Check if all required services are up
int required = AUTH_SERVICE | PRICING_SERVICE | RFQ_SERVICE;
boolean allReady = (initialized & required) == required;
```
**Where it applies**: kACE StaticCacheOrchestrator startup tracking, Spring Boot service health bitmask.
> 🏭 **Industry Example**: Redis uses bitmaps (SET/GETBIT operations) for compact boolean state storage — e.g., tracking which user IDs have seen a notification (1 bit per user = 500MB for 4B users). Bloom filters (used by Chrome safe browsing, Cassandra) use bitmask state compression.
> 🏦 **kACE Context**: kACE StaticCacheOrchestrator startup tracking — bitmask of which microservices have completed cache pre-loading.


---

### Use Case 4: XOR for Data Integrity Check
**Problem**: Detect if any single bit was flipped during Kafka message transmission.

```java
// Simple parity check using XOR
int computeChecksum(byte[] data) {
    int checksum = 0;
    for (byte b : data) checksum ^= b;
    return checksum;
}

// Verify integrity
boolean isValid = computeChecksum(received) == expectedChecksum;

// Find which byte was corrupted
int corruptedByte = computeChecksum(received) ^ expectedChecksum;
```
**Where it applies**: Kafka message integrity, WebSocket frame checksum, JWT payload validation.
> 🏭 **Industry Example**: Ethernet frames use XOR-based CRC checksums for error detection. RAID-5 disk arrays use XOR parity bits for fault tolerance — losing one disk, XOR reconstruction recovers data. TCP/IP packet headers include XOR-based checksums.
> 🏦 **kACE Context**: Kafka message integrity validation — XOR checksum verification for trade event payloads.


---

### Use Case 5: Bitmask DP — Optimal Team Assignment
**Problem**: Assign N tasks to N team members optimally (minimum total time), each member does exactly one task.

```java
// dp[mask] = min cost to assign tasks represented by mask to first popcount(mask) members
int n = members.length;
int[] dp = new int[1 << n];
Arrays.fill(dp, Integer.MAX_VALUE);
dp[0] = 0;
for (int mask = 0; mask < (1 << n); mask++) {
    if (dp[mask] == Integer.MAX_VALUE) continue;
    int memberIdx = Integer.bitCount(mask); // next member to assign
    if (memberIdx == n) continue;
    for (int task = 0; task < n; task++) {
        if ((mask & (1 << task)) == 0) { // task not yet assigned
            dp[mask | (1 << task)] = Math.min(
                dp[mask | (1 << task)],
                dp[mask] + cost[memberIdx][task]
            );
        }
    }
}
return dp[(1 << n) - 1]; // all tasks assigned
```
**Where it applies**: Optimal sprint task assignment in kACE team management, resource allocation.
> 🏭 **Industry Example**: Google's interview scheduling system uses bitmask DP for optimal interviewer-to-candidate assignment. Airline crew scheduling uses bitmask DP for optimal flight-to-crew assignment. Uber's driver-trip assignment uses bitmask DP for small batch optimizations.
> 🏦 **kACE Context**: Optimal sprint task assignment in kACE team management — minimizing total effort across 12 team members.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Single Number | XOR | Easy |
| 2 | Number of 1 Bits | Brian Kernighan | Easy |
| 3 | Power of Two | n & (n-1) | Easy |
| 4 | Reverse Bits | Bit iteration | Easy |
| 5 | Missing Number | XOR / Math | Easy |
| 6 | Single Number II | Bit counting | Medium |
| 7 | Sum of Two Integers (no +) | XOR + carry | Medium |
| 8 | Counting Bits | DP + bit trick | Easy |
| 9 | Subsets via Bitmask | Enumeration | Medium |
| 10 | Maximum XOR of Two Numbers | Trie / Greedy bits | Medium |

---

## ⚠️ Common Mistakes

- Integer overflow: `1 << 31` is negative in Java — use `1L << 31` for long
- `~n` in Java gives two's complement — `~0 = -1`, not `Integer.MAX_VALUE`
- Right shift: `>>` is **signed** (preserves sign bit), `>>>` is **unsigned** — use `>>>` for bit manipulation
- XOR swap: doesn't work if `a` and `b` are the same variable/reference
- Bitmask DP: state space is `2^n` — only feasible for n ≤ 20 typically
- Checking bit: use `(n >> i) & 1` not `n & i` (common mistake when i > 1)
