package diffiehellman;

import java.math.BigInteger;
import java.util.Random;

/**
 * Standalone object implementing the supplied classroom Diffie-Hellman algorithm.
 * P/G are agreed publicly; PA/PB are private; pa/pb are exchanged; SA/SB are secrets.
 * The original's Math.pow(base, exponent) % P is evaluated using exact integer
 * modular exponentiation to avoid floating-point overflow / rounding errors.
 * Input verification is deliberately handled by InputValidator in the UI layer.
 * This is a small-number classroom demonstration, not production cryptography.
 */
public final class DiffieHellmann {
    private final int P, G, PA, PB;
    private final int pa, pb, SA, SB;

    public DiffieHellmann(int P, int G, int PA, int PB) {
        this.P = P;
        this.G = G;
        this.PA = PA;
        this.PB = PB;
        // Original: pa = (int)Math.pow(G, PA)%P; pb = (int)Math.pow(G, PB)%P;
        this.pa = powerMod(G, PA, P);
        this.pb = powerMod(G, PB, P);
        // Original: SA = (int)Math.pow(pb, PA)%P; SB = (int)Math.pow(pa, PB)%P;
        this.SA = powerMod(pb, PA, P);
        this.SB = powerMod(pa, PB, P);
    }

    public static int randomPrivateKey(Random random) {
        return 1 + random.nextInt(9); // exactly the supplied range: 1 through 9
    }

    private static int powerMod(int base, int exponent, int modulus) {
        return BigInteger.valueOf(base).modPow(BigInteger.valueOf(exponent), BigInteger.valueOf(modulus)).intValueExact();
    }

    public int getP() { return P; }
    public int getG() { return G; }
    public int getPA() { return PA; }
    public int getPB() { return PB; }
    public int getPa() { return pa; }
    public int getPb() { return pb; }
    public int getSA() { return SA; }
    public int getSB() { return SB; }
    public boolean secretsMatch() { return SA == SB; }
}
