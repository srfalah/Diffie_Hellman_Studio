package diffiehellman;

import java.util.Random;

/** UI input checking, intentionally separate from the supplied algorithm object. */
public final class InputValidator {
    private InputValidator() {}

    public record Values(int P, int G, int PA, int PB) {}

    public static Values validate(String pText, String gText, String aliceText, String bobText) {
        int P = parse(pText, "P");
        int G = parse(gText, "G");
        int PA = parse(aliceText, "Alice private key (PA)");
        int PB = parse(bobText, "Bob private key (PB)");
        if (P < 5 || P > 997 || !isPrime(P))
            throw new IllegalArgumentException("P must be a prime number from 5 to 997.");
        if (G < 2 || G >= P)
            throw new IllegalArgumentException("G must satisfy 2 ≤ G < P.");
        if (!isPrimitiveRoot(G, P))
            throw new IllegalArgumentException("G must be a generator (primitive root) modulo P.");
        if (PA < 1 || PA > 9 || PB < 1 || PB > 9)
            throw new IllegalArgumentException("PA and PB must each be an integer from 1 to 9, as in the original code.");
        return new Values(P, G, PA, PB);
    }


    /** Partial validation at focus boundaries; empty means the student has not entered a value yet. */
    public static String primeError(String raw) {
        if (raw == null || raw.isBlank()) return "P is required. Enter a prime number from 5 to 997.";
        Integer n = tryInt(raw);
        if (n == null) return "P must be a whole number from 5 to 997.";
        return n >= 5 && n <= 997 && isPrime(n) ? null : "P must be a prime number from 5 to 997.";
    }

    public static String generatorError(String raw, int prime) {
        if (raw == null || raw.isBlank()) return "G is required. Enter a primitive root modulo P.";
        Integer n = tryInt(raw);
        if (n == null) return "G must be a whole number.";
        if (n < 2 || n >= prime) return "G must satisfy 2 ≤ G < P.";
        return isPrimitiveRoot(n, prime) ? null : "G must be a primitive root modulo P.";
    }

    public static String privateError(String raw, String name) {
        Integer n = tryInt(raw);
        return n != null && n >= 1 && n <= 9 ? null : name + " must be a whole number from 1 to 9.";
    }

    public static Integer tryInt(String raw) {
        if (raw == null || !raw.trim().matches("[0-9]+")) return null;
        try { return Integer.valueOf(raw.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /** Closest available prime, preferring the larger one in a tie. */
    public static int suggestPrime(String raw) {
        Integer original = tryInt(raw);
        int target = original == null ? 23 : Math.max(5, Math.min(997, original));
        for (int distance = 0; distance <= 997; distance++) {
            int above = target + distance;
            if (above <= 997 && isPrime(above)) return above;
            int below = target - distance;
            if (below >= 5 && isPrime(below)) return below;
        }
        return 23;
    }

    public static int suggestGenerator(String raw, int prime) {
        Integer original = tryInt(raw);
        int target = original == null ? 2 : Math.max(2, Math.min(prime - 1, original));
        for (int distance = 0; distance < prime; distance++) {
            int above = target + distance;
            if (above < prime && isPrimitiveRoot(above, prime)) return above;
            int below = target - distance;
            if (below >= 2 && isPrimitiveRoot(below, prime)) return below;
        }
        throw new IllegalArgumentException("No generator found for P.");
    }

    public static int suggestPrivate(String raw) {
        Integer n = tryInt(raw);
        return n == null ? 5 : Math.max(1, Math.min(9, n));
    }

    private static int parse(String raw, String label) {
        if (raw == null || !raw.trim().matches("[0-9]+"))
            throw new IllegalArgumentException(label + " must contain a whole positive number.");
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " is too large."); }
    }

    public static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int d = 2; d * d <= n; d++) if (n % d == 0) return false;
        return true;
    }

    // A valid primitive root ensures the public values illustrate full-group DH.
    public static boolean isPrimitiveRoot(int g, int p) {
        if (!isPrime(p) || g < 2 || g >= p) return false;
        int phi = p - 1;
        for (int q = 2; q <= phi; q++) {
            if (phi % q == 0 && isPrime(q) && powMod(g, phi / q, p) == 1) return false;
        }
        return true;
    }

    private static int powMod(int base, int exp, int modulus) {
        long result = 1, x = base % modulus;
        while (exp > 0) {
            if ((exp & 1) == 1) result = result * x % modulus;
            x = x * x % modulus;
            exp >>= 1;
        }
        return (int) result;
    }

    public static Values generate(Random random) {
        int[] primes = {11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};
        int p = primes[random.nextInt(primes.length)];
        int g = 2 + random.nextInt(p - 2);
        while (!isPrimitiveRoot(g, p)) g = 2 + random.nextInt(p - 2);
        return new Values(p, g, DiffieHellmann.randomPrivateKey(random), DiffieHellmann.randomPrivateKey(random));
    }
}
