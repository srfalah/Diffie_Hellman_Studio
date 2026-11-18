package diffiehellman;

import java.util.Random;

/** Run independently without JavaFX: java -ea diffiehellman.ModelSelfTest */
public final class ModelSelfTest {
    public static void main(String[] args) {
        InputValidator.Values v = InputValidator.validate("23", "5", "6", "9");
        DiffieHellmann dh = new DiffieHellmann(v.P(), v.G(), v.PA(), v.PB());
        assert dh.getPa() == 8 : "Alice public key";
        assert dh.getPb() == 11 : "Bob public key";
        assert dh.getSA() == 9 && dh.getSB() == 9 : "Shared secret";
        for (int i = 0; i < 500; i++) {
            InputValidator.Values t = InputValidator.generate(new Random(i));
            InputValidator.validate("" + t.P(), "" + t.G(), "" + t.PA(), "" + t.PB());
            assert new DiffieHellmann(t.P(), t.G(), t.PA(), t.PB()).secretsMatch();
        }
        rejected("21", "2", "2", "2");
        rejected("23", "23", "2", "2");
        rejected("23", "4", "2", "2");
        rejected("23", "5", "0", "2");
        rejected("23", "5", "2", "10");
        rejected("abc", "5", "2", "2");
        assert InputValidator.suggestPrime("10") == 11;
        assert InputValidator.isPrime(InputValidator.suggestPrime("invalid"));
        assert InputValidator.isPrimitiveRoot(InputValidator.suggestGenerator("4", 23), 23);
        assert InputValidator.suggestPrivate("15") == 9;
        assert InputValidator.primeError("10") != null;
        assert InputValidator.generatorError("4", 23) != null;
        assert InputValidator.primeError("23") == null;
        assert InputValidator.generatorError("5", 23) == null;
        System.out.println("PASS: classroom example, 500 generated cases, invalid-input rejection");
    }
    private static void rejected(String p, String g, String a, String b) {
        try { InputValidator.validate(p, g, a, b); throw new AssertionError("Accepted invalid input"); }
        catch (IllegalArgumentException expected) { /* good */ }
    }
}
