# Diffie–Hellman Key Exchange Studio

Java 21 / JavaFX 21 teaching app. Its visual identity is intentionally retained from the supplied **RSA Ciphering Studio**: same CSS, header, frameless title bar, dark-mode toggle, two-column cards, controls, logo and footer.

## Run in IntelliJ IDEA
1. Open this folder as a **Maven project**.
2. Set **Project SDK = JDK 21** (Azul Zulu JDK 21 or equivalent).
3. Wait for IntelliJ to download the JavaFX Maven dependencies (first run requires internet).
4. In the Maven tool window run **Plugins → javafx → javafx:run**, or use terminal:
   ```bash
   mvn clean javafx:run
   ```
5. App opens maximized. A 1120 × 700 minimum window is enforced; scroll only on smaller displays.

Windows: `run-windows.bat` · Linux/macOS: `./run-linux.sh`

## Classroom flow
1. Enter **P**, **G**, **PA**, **PB** manually, click **Generate numbers**, or choose **Example: P=23, G=5**.
2. Click **Generate public keys**. Inputs are checked before the model is created.
3. Click **Animate exchange** to see `pa` move Alice → Bob and `pb` Bob → Alice.
4. Watch the two independently computed secrets, `SA` and `SB`, appear and match. Replay is supported.
5. Changing any field clears all stale public/secret values and cancels the animation.

**Original teaching variables and formulas (preserved):** `P, G, PA, PB, pa, pb, SA, SB`;
`pa = G^PA mod P`; `pb = G^PB mod P`; `SA = pb^PA mod P`; `SB = pa^PB mod P`.
Random secrets use `1 + random.nextInt(9)` (range 1–9) as in the uploaded code.

**One necessary numerical fix:** the supplied `(int)Math.pow(... ) % P` can overflow or round incorrectly. This version computes the *same mathematical expression* with exact `BigInteger.modPow`, inside the separate `DiffieHellmann` class. Validation is entirely in `InputValidator` and the JavaFX controller. Manual entry requires a prime P (5–997), a primitive root G (2 ≤ G < P), and PA/PB (1–9). Public values are small for explaining the algorithm; **do not use this demo for real cryptographic security**.

## Files
- `DiffieHellmann.java` — standalone algorithm object only.
- `InputValidator.java` — input checks and sample generator (UI-support layer).
- `StudioController.java` — form state, CSS classes, window controls and animated exchange.
- `DiffieView.fxml` — layout modelled on the original RSA view.
- `style.css` / `studio-mark.png` — original visual style and image, with a small DH-only CSS appendix.
- `ModelSelfTest.java` — no-dependency model + validation checks.

## Model-only test (JDK 21)
```bash
mkdir -p test-classes
javac -d test-classes src/main/java/diffiehellman/DiffieHellmann.java src/main/java/diffiehellman/InputValidator.java src/test/java/diffiehellman/ModelSelfTest.java
java -ea -cp test-classes diffiehellman.ModelSelfTest
```

## Focus-based input validation

- The `P` and `G` fields are checked when the user leaves each field, before public-key generation.
- Invalid `P` blocks the subsequent numeric fields. Invalid `G` permits editing `P` or `G` but blocks private keys and the compute action.
- Private-key inputs are also checked when the user leaves each field.
- A highlighted field, explicit error, and **Use N** button propose a valid alternative; applying the suggestion immediately rechecks the inputs.
- **Example**, **Generate numbers**, and **Reset** remain available to recover without getting stuck.
- The mathematical algorithm remains isolated in `DiffieHellmann.java`; UI validation and suggestions live in `InputValidator.java` / `StudioController.java`.
