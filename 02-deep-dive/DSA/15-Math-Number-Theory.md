# 📚 DSA Deep Dive — Math & Number Theory

---

## 🧠 Core Topics

1. GCD & LCM
2. Prime Numbers & Sieve
3. Modular Arithmetic
4. Fast Power (Exponentiation by Squaring)
5. Combinatorics (nCr, Pascal's Triangle)
6. Number Properties

---

## 🔑 GCD & LCM

### Euclidean Algorithm — GCD
```java
// GCD(a, b) = GCD(b, a % b), base: GCD(a, 0) = a
int gcd(int a, int b) {
    return b == 0 ? a : gcd(b, a % b);
}
// Iterative
int gcdIter(int a, int b) {
    while (b != 0) { int t = b; b = a % b; a = t; }
    return a;
}
// Time: O(log(min(a,b)))
```

### LCM
```java
long lcm(long a, long b) {
    return a / gcd(a, b) * b; // divide first to prevent overflow
}
```

---

## 🔑 Prime Numbers

### Is Prime?
```java
boolean isPrime(int n) {
    if (n < 2) return false;
    if (n == 2) return true;
    if (n % 2 == 0) return false;
    for (int i = 3; i * i <= n; i += 2) // check up to √n
        if (n % i == 0) return false;
    return true;
}
// Time: O(√n)
```

### Sieve of Eratosthenes — All Primes up to N
```java
boolean[] sieve(int n) {
    boolean[] isPrime = new boolean[n + 1];
    Arrays.fill(isPrime, true);
    isPrime[0] = isPrime[1] = false;
    for (int i = 2; i * i <= n; i++) {
        if (isPrime[i]) {
            for (int j = i * i; j <= n; j += i)
                isPrime[j] = false;
        }
    }
    return isPrime;
}
// Time: O(n log log n), Space: O(n)
```

### Prime Factorization
```java
List<Integer> primeFactors(int n) {
    List<Integer> factors = new ArrayList<>();
    for (int i = 2; i * i <= n; i++) {
        while (n % i == 0) { factors.add(i); n /= i; }
    }
    if (n > 1) factors.add(n); // remaining prime factor
    return factors;
}
```

---

## 🔑 Modular Arithmetic

### Why mod?
Large numbers overflow — take mod at every step.

```java
// Addition: (a + b) % m = ((a % m) + (b % m)) % m
// Subtraction: (a - b) % m = ((a % m) - (b % m) + m) % m  ← +m to handle negative
// Multiplication: (a * b) % m = ((a % m) * (b % m)) % m
// Division: requires modular inverse (only if m is prime → Fermat's little theorem)

static final int MOD = 1_000_000_007; // common in competitive programming

long modAdd(long a, long b) { return (a % MOD + b % MOD) % MOD; }
long modMul(long a, long b) { return (a % MOD) * (b % MOD) % MOD; }
long modSub(long a, long b) { return ((a % MOD) - (b % MOD) + MOD) % MOD; }
```

### Modular Inverse (Fermat's Little Theorem)
```java
// a^(-1) mod p = a^(p-2) mod p, when p is prime
long modInverse(long a, long p) {
    return fastPow(a, p - 2, p);
}
```

---

## 🔑 Fast Power (Binary Exponentiation)

```java
// Compute base^exp % mod in O(log exp)
long fastPow(long base, long exp, long mod) {
    long result = 1;
    base %= mod;
    while (exp > 0) {
        if ((exp & 1) == 1) result = result * base % mod; // odd exponent
        base = base * base % mod;
        exp >>= 1;
    }
    return result;
}
// Without mod (for small numbers)
long pow(long base, int exp) {
    long result = 1;
    while (exp > 0) {
        if ((exp & 1) == 1) result *= base;
        base *= base; exp >>= 1;
    }
    return result;
}
```

---

## 🔑 Combinatorics

### nCr (Combinations)
```java
// Pascal's Triangle — precompute all nCr up to N
long[][] precomputeCombinations(int n) {
    long[][] C = new long[n+1][n+1];
    for (int i = 0; i <= n; i++) {
        C[i][0] = 1;
        for (int j = 1; j <= i; j++)
            C[i][j] = (C[i-1][j-1] + C[i-1][j]) % MOD;
    }
    return C;
}

// Single nCr using factorial + modular inverse
long nCr(int n, int r) {
    if (r > n) return 0;
    long num = 1, den = 1;
    for (int i = 0; i < r; i++) {
        num = num * (n - i) % MOD;
        den = den * (i + 1) % MOD;
    }
    return num * modInverse(den, MOD) % MOD;
}
```

### Fibonacci (Fast — Matrix Exponentiation)
```java
// Fibonacci in O(log n) via fast matrix power
// F(n) = [[1,1],[1,0]]^n [0][1]
long fibonacci(int n) {
    if (n <= 1) return n;
    long[][] matrix = {{1,1},{1,0}};
    long[][] result = matpow(matrix, n-1);
    return result[0][0];
}
```

---

## 🔑 Number Properties

```java
// Digit sum
int digitSum(int n) {
    int sum = 0;
    while (n > 0) { sum += n % 10; n /= 10; }
    return sum;
}

// Reverse digits
int reverseDigits(int n) {
    int rev = 0;
    while (n != 0) { rev = rev * 10 + n % 10; n /= 10; }
    return rev;
}

// Count digits
int numDigits(int n) { return (int)Math.log10(n) + 1; }

// Integer square root (without Math.sqrt for precision)
int isqrt(int n) {
    int lo = 0, hi = (int)Math.sqrt(n) + 1;
    while (lo < hi) {
        int mid = lo + (hi - lo + 1) / 2;
        if ((long)mid * mid <= n) lo = mid;
        else hi = mid - 1;
    }
    return lo;
}
```

---

## 🌍 Real-World Use Cases

### Use Case 1: GCD for FX Rate Normalization
**Problem**: Simplify FX rate ratios (e.g., 150/100 → 3/2) for display.

```java
// Simplify EUR/USD rate ratio
int normalize(int num, int den) {
    int g = gcd(Math.abs(num), Math.abs(den));
    return new int[]{num/g, den/g}; // simplified fraction
}
// 1.5050 USD per EUR → store as {10100, 6700} → GCD → {1010, 670} → {101, 67}
```
**Where it applies**: FX rate display normalization, option strike price simplification in kACE.
> 🏭 **Industry Example**: Python's `fractions.Fraction` class uses GCD for automatic simplification. React's responsive grid system uses GCD to compute column ratios. Music theory applications use GCD for rhythm quantization (e.g., 6/8 time simplified from 12/16).
> 🏦 **kACE Context**: FX rate display normalization — simplifying EUR/USD rate ratios for compact display on the trading screen.


---

### Use Case 2: Sieve for Risk Bucketing (Prime-based Hashing)
**Problem**: Use prime numbers as hash seeds to minimize collision in trade ID bucketing.

```java
// Get first K primes as hash seeds
boolean[] isPrime = sieve(1000);
List<Integer> primes = new ArrayList<>();
for (int i = 2; i < 1000 && primes.size() < K; i++)
    if (isPrime[i]) primes.add(i);
// Use primes as bucket multipliers → minimal collision
int bucketId = (tradeId * primes.get(0)) % numBuckets;
```
**Where it applies**: Kafka partition key hashing, trade ID bucketing for parallel processing.
> 🏭 **Industry Example**: RSA encryption (used in HTTPS/TLS everywhere) requires large prime generation — Sieve of Eratosthenes is the standard approach. Cryptographic libraries (OpenSSL, BouncyCastle) use optimized prime sieves. Diffie-Hellman key exchange requires prime number generation at scale.
> 🏦 **kACE Context**: Prime-based hash seeds for trade ID bucketing to minimize collision in parallel processing.


---

### Use Case 3: Fast Power for Option Greeks
**Problem**: Compute compound growth factor (1+r)^n for option pricing quickly.

```java
// Compute (1 + dailyRate)^n efficiently
// Store as integer: rate = 1001 means 1.001
long compoundFactor = fastPow(1001L, days, MOD);
// Used in: Black-Scholes discount factor, binomial tree pricing
```
**Where it applies**: kACE FX option pricing — discount factors, forward rate computation.
> 🏭 **Industry Example**: Bitcoin mining uses fast modular exponentiation in SHA-256 proof-of-work. RSA encryption/decryption is entirely based on `base^exp % mod` computed via fast power. TLS handshake Diffie-Hellman uses fast power hundreds of times per second.
> 🏦 **kACE Context**: kACE FX option pricing — computing compound growth factors `(1+r)^n` for discount factors.


---

### Use Case 4: LCM for Synchronized Batch Jobs
**Problem**: Find when two scheduled jobs (running every A and B seconds) will sync.

```java
// Jobs sync at LCM(A, B) seconds
long syncInterval = lcm(jobA.intervalSeconds, jobB.intervalSeconds);
System.out.println("Jobs sync every " + syncInterval + " seconds");
// e.g., job every 6s and every 4s → sync every 12s
```
**Where it applies**: kACE StaticCacheOrchestrator refresh intervals, Kafka consumer sync scheduling.
> 🏭 **Industry Example**: Operating system schedulers use LCM to find when periodic tasks next align (Rate Monotonic Scheduling). Cron job systems find common execution windows using LCM. Video game engines synchronize physics updates and rendering cycles using LCM of their frequencies.
> 🏦 **kACE Context**: kACE StaticCacheOrchestrator — synchronizing refresh intervals between market data, dropdown, and layout config caches.


---

### Use Case 5: nCr for Strategy Count (Combinatorics)
**Problem**: How many ways can a trader choose 2 legs from 5 available legs for a spread?

```java
// C(5,2) = 10 possible 2-leg spreads from 5 available legs
long strategies = nCr(availableLegs, legsPerStrategy);
System.out.println("Possible strategies: " + strategies);

// How many ways to assign 3 traders to 8 RFQs?
long assignments = nCr(8, 3); // C(8,3) = 56
```
**Where it applies**: kACE strategy combination analysis, team workload distribution planning.
> 🏭 **Industry Example**: A/B testing platforms (Optimizely, VWO) use combinatorics to calculate the number of variant combinations. Poker hand probability calculators use nCr extensively. Machine learning cross-validation uses nCr to count train/test split combinations.
> 🏦 **kACE Context**: kACE strategy combination analysis — counting how many 2-leg spreads are possible from 8 available currency pairs.


---

## 🏋️ Practice Problems

| # | Problem | Pattern | Difficulty |
|---|---------|---------|------------|
| 1 | Count Primes | Sieve | Medium |
| 2 | Happy Number | Digit sum cycle | Easy |
| 3 | Ugly Number | Prime factors | Easy |
| 4 | Power(x, n) | Fast power | Medium |
| 5 | Sqrt(x) | Binary search / Math | Easy |
| 6 | Reverse Integer | Digit manipulation | Medium |
| 7 | Excel Sheet Column Number | Base conversion | Easy |
| 8 | Factorial Trailing Zeros | Count factor 5 | Medium |
| 9 | GCD of Strings | GCD + string | Easy |
| 10 | Super Pow | Modular fast power | Medium |

---

## ⚠️ Common Mistakes

- `isPrime` check: loop to `i * i <= n`, NOT `i <= n` (O(√n) vs O(n))
- Sieve: start marking from `i*i`, not `2*i` (smaller multiples already marked)
- Modular subtraction: always add `MOD` before taking mod to avoid negative results
- Fast power: use `long` for intermediate products to prevent overflow
- LCM: divide by GCD **before** multiplying — `a / gcd(a,b) * b` NOT `a * b / gcd(a,b)`
- `Math.sqrt()` has floating point precision issues — use integer binary search for exact sqrt
