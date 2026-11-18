package diffiehellman;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

/** Presentation, validation and exchange animation only: no DH mathematics here. */
public final class StudioController {
    @FXML private BorderPane rootPane;
    @FXML private HBox customTitleBar;
    @FXML private Button maximizeWinBtn, themeButton, exchangeButton, replayButton, computeButton, suggestionButton;
    @FXML private TextField pInput, gInput, aliceInput, bobInput;
    @FXML private Label validationBadge, exchangeBadge, inputFeedback, footerStatus;
    @FXML private Label step1, step2, step3, step4;
    @FXML private Label alicePublicFormula, bobPublicFormula, alicePublicValue, bobPublicValue;
    @FXML private Label alicePrivateLabel, bobPrivateLabel, alicePublicLabel, bobPublicLabel;
    @FXML private Label forwardArrow, backArrow, exchangeMessage, animationHint;
    @FXML private Label aliceSecretFormula, bobSecretFormula, aliceSecretValue, bobSecretValue;
    @FXML private Label matchBadge, matchDetail;
    @FXML private StackPane animationHost;
    @FXML private ScrollPane mainScrollPane;
    @FXML private GridPane contentGrid;

    private final Random random = new Random();
    private DiffieHellmann current;
    private ParallelTransition animation;
    private Label forwardToken, backToken;
    private boolean programmaticUpdate;
    private TextField invalidField;
    private TextField suggestedField;
    private int suggestedNumber;
    private boolean pVisited, gVisited, aliceVisited, bobVisited;
    private double dragX, dragY;

    @FXML private void initialize() {
        for (TextField field : new TextField[]{pInput, gInput, aliceInput, bobInput}) {
            field.textProperty().addListener((observable, oldValue, newValue) -> {
                if (programmaticUpdate) return;
                invalidateResults();
                // When the user fixes a blocked field, unlock immediately without requiring another click.
                if (invalidField != null) {
                    checkVisitedValues();
                }
            });
            field.focusedProperty().addListener((observable, wasFocused, focused) -> {
                if (!focused && !programmaticUpdate) {
                    setVisited(field);
                    checkVisitedValues();
                }
            });
        }
        mainScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) -> fillViewportHeight());
        Platform.runLater(this::fillViewportHeight);
        showStep(1);
    }

    private void setVisited(TextField field) {
        if (field == pInput) pVisited = true;
        else if (field == gInput) gVisited = true;
        else if (field == aliceInput) aliceVisited = true;
        else if (field == bobInput) bobVisited = true;
    }

    /** Validate committed fields in their teaching order, never while entering an unfinished number. */
    private void checkVisitedValues() {
        String error = null;
        TextField problem = null;
        int suggestion = 0;
        if (pVisited) {
            error = InputValidator.primeError(pInput.getText());
            if (error != null) {
                problem = pInput;
                suggestion = InputValidator.suggestPrime(pInput.getText());
            }
        }
        Integer p = InputValidator.tryInt(pInput.getText());
        if (error == null && gVisited && p != null && InputValidator.primeError(pInput.getText()) == null) {
            error = InputValidator.generatorError(gInput.getText(), p);
            if (error != null) {
                problem = gInput;
                suggestion = InputValidator.suggestGenerator(gInput.getText(), p);
            }
        }
        if (error == null && aliceVisited) {
            error = InputValidator.privateError(aliceInput.getText(), "PA");
            if (error != null) {
                problem = aliceInput;
                suggestion = InputValidator.suggestPrivate(aliceInput.getText());
            }
        }
        if (error == null && bobVisited) {
            error = InputValidator.privateError(bobInput.getText(), "PB");
            if (error != null) {
                problem = bobInput;
                suggestion = InputValidator.suggestPrivate(bobInput.getText());
            }
        }
        invalidField = problem;
        suggestedField = problem;
        suggestedNumber = suggestion;
        for (TextField field : new TextField[]{pInput, gInput, aliceInput, bobInput}) {
            field.getStyleClass().remove("input-invalid");
        }
        // Keep P editable when G is invalid: changing P can also resolve the generator problem.
        pInput.setDisable(problem == aliceInput || problem == bobInput);
        aliceInput.setDisable(problem != null && problem != aliceInput);
        bobInput.setDisable(problem != null && problem != bobInput);
        gInput.setDisable(problem != null && problem != gInput);
        computeButton.setDisable(problem != null);
        if (problem != null) {
            problem.getStyleClass().add("input-invalid");
            inputFeedback.setText(error + "  Suggested " + (problem == pInput ? "P" : problem == gInput ? "G" : problem == aliceInput ? "PA" : "PB") + " = " + suggestion + ".");
            style(inputFeedback, "validation-invalid");
            validationBadge.setText("Invalid input");
            style(validationBadge, "status-badge-neutral");
            suggestionButton.setText("Use " + suggestion);
            suggestionButton.setVisible(true);
            suggestionButton.setManaged(true);
            footerStatus.setText("INPUT ERROR  •  Correct the highlighted field to continue");
            TextField expected = problem;
            Platform.runLater(() -> {
                if (invalidField == expected && rootPane.getScene() != null) {
                    var owner = rootPane.getScene().getFocusOwner();
                    // Only return focus when the student tries moving to an unrelated field.
                    if (owner instanceof TextField && owner != expected
                        && !(expected == gInput && owner == pInput)) expected.requestFocus();
                }
            });
        } else {
            suggestionButton.setVisible(false);
            suggestionButton.setManaged(false);
            if (current == null) {
                validationBadge.setText("Awaiting input");
                style(validationBadge, "status-badge-neutral");
                style(inputFeedback, "validation-neutral");
                inputFeedback.setText("Values accepted so far. Continue entering the numbers.");
            }
        }
    }

    @FXML private void applySuggestion() {
        if (suggestedField == null) return;
        TextField target = suggestedField;
        target.setText(Integer.toString(suggestedNumber));
        checkVisitedValues();
        target.requestFocus();
        target.positionCaret(target.getText().length());
    }

    private void fillViewportHeight() {
        if (mainScrollPane == null || contentGrid == null) return;
        double viewportHeight = mainScrollPane.getViewportBounds().getHeight();
        double topBottomPadding = 28; // 14 top + 14 bottom from GridPane padding
        if (viewportHeight > 0) {
            contentGrid.setMinHeight(viewportHeight - topBottomPadding);
            contentGrid.setPrefHeight(viewportHeight - topBottomPadding);
        }
    }

    @FXML private void example() {
        assign(23, 5, 6, 9);
        footerStatus.setText("EXAMPLE LOADED  •  Press Generate public keys");
    }

    @FXML private void generate() {
        InputValidator.Values values = InputValidator.generate(random);
        assign(values.P(), values.G(), values.PA(), values.PB());
        footerStatus.setText("RANDOM NUMBERS GENERATED  •  Press Generate public keys");
    }

    private void assign(int P, int G, int PA, int PB) {
        programmaticUpdate = true;
        try {
            pInput.setText(Integer.toString(P));
            gInput.setText(Integer.toString(G));
            aliceInput.setText(Integer.toString(PA));
            bobInput.setText(Integer.toString(PB));
        } finally { programmaticUpdate = false; }
        clearValidation();
        invalidateResults();
        inputFeedback.setText("Numbers ready. Press Generate public keys to calculate.");
    }

    @FXML private void compute() {
        // Calculation is separate from validation; the latter already runs on focus loss.
        pVisited = gVisited = aliceVisited = bobVisited = true;
        checkVisitedValues();
        if (invalidField != null) return;
        stopAnimation();
        try {
            InputValidator.Values v = InputValidator.validate(pInput.getText(), gInput.getText(), aliceInput.getText(), bobInput.getText());
            current = new DiffieHellmann(v.P(), v.G(), v.PA(), v.PB());
            alicePublicFormula.setText("pa = " + v.G() + "^" + v.PA() + " mod " + v.P());
            bobPublicFormula.setText("pb = " + v.G() + "^" + v.PB() + " mod " + v.P());
            alicePublicValue.setText(Integer.toString(current.getPa()));
            bobPublicValue.setText(Integer.toString(current.getPb()));
            alicePrivateLabel.setText(Integer.toString(v.PA()));
            bobPrivateLabel.setText(Integer.toString(v.PB()));
            alicePublicLabel.setText(Integer.toString(current.getPa()));
            bobPublicLabel.setText(Integer.toString(current.getPb()));
            forwardArrow.setText("pa=" + current.getPa() + "  ─────▶");
            backArrow.setText("◀─────  pb=" + current.getPb());
            validationBadge.setText("Valid parameters");
            style(validationBadge, "status-badge-ready");
            style(inputFeedback, "validation-valid");
            inputFeedback.setText("Validated: P is prime; G generates the group; private keys are within 1–9.");
            exchangeBadge.setText("Ready to exchange");
            style(exchangeBadge, "status-badge-teal");
            exchangeMessage.setText("Values computed locally");
            animationHint.setText("Press Animate exchange to send pa → Bob and pb → Alice");
            exchangeButton.setDisable(false);
            replayButton.setDisable(true);
            showStep(2);
            footerStatus.setText("KEYS GENERATED  •  Public values calculated; private values remain local");
        } catch (IllegalArgumentException ex) {
            invalidateResults();
            inputFeedback.setText(ex.getMessage());
            style(inputFeedback, "validation-invalid");
            validationBadge.setText("Invalid input");
            style(validationBadge, "status-badge-neutral");
            footerStatus.setText("INPUT ERROR  •  Correct the values and try again");
        }
    }

    @FXML private void exchange() {
        if (current == null) return;
        stopAnimation();
        exchangeButton.setDisable(true);
        replayButton.setDisable(true);
        showStep(3);
        exchangeBadge.setText("Exchanging…");
        style(exchangeBadge, "status-badge-teal");
        exchangeMessage.setText("Only PUBLIC keys cross the channel");
        animationHint.setVisible(false);
        animationHint.setManaged(false);

        forwardToken = new Label("pa = " + current.getPa() + "  →");
        backToken = new Label("←  pb = " + current.getPb());
        forwardToken.getStyleClass().add("flying-token");
        backToken.getStyleClass().addAll("flying-token", "flying-token-bob");
        animationHost.getChildren().addAll(forwardToken, backToken);
        double travel = Math.max(180, animationHost.getWidth() - 150);
        forwardToken.setTranslateX(-travel / 2);
        forwardToken.setTranslateY(-22);
        backToken.setTranslateX(travel / 2);
        backToken.setTranslateY(22);
        TranslateTransition leftToRight = new TranslateTransition(Duration.millis(2150), forwardToken);
        leftToRight.setFromX(-travel / 2);
        leftToRight.setToX(travel / 2);
        leftToRight.setInterpolator(Interpolator.EASE_BOTH);
        TranslateTransition rightToLeft = new TranslateTransition(Duration.millis(2150), backToken);
        rightToLeft.setFromX(travel / 2);
        rightToLeft.setToX(-travel / 2);
        rightToLeft.setInterpolator(Interpolator.EASE_BOTH);
        animation = new ParallelTransition(leftToRight, rightToLeft);
        animation.setOnFinished(event -> finishExchange());
        animation.playFromStart();
        footerStatus.setText("EXCHANGE IN PROGRESS  •  Alice sends pa; Bob sends pb");
    }

    private void finishExchange() {
        if (current == null) return;
        cleanupTokens();
        animation = null;
        animationHint.setManaged(true);
        animationHint.setVisible(true);
        animationHint.setText("✓ pa reached Bob  •  pb reached Alice");
        aliceSecretFormula.setText("SA = " + current.getPb() + "^" + current.getPA() + " mod " + current.getP());
        bobSecretFormula.setText("SB = " + current.getPa() + "^" + current.getPB() + " mod " + current.getP());
        aliceSecretValue.setText(Integer.toString(current.getSA()));
        bobSecretValue.setText(Integer.toString(current.getSB()));
        boolean equal = current.secretsMatch();
        matchBadge.setText(equal ? "MATCH ✓" : "MISMATCH ✕");
        style(matchBadge, equal ? "status-badge-ready" : "status-badge-neutral");
        matchDetail.setText(equal
                ? "SA = SB = " + current.getSA() + "   •   Shared secret established independently."
                : "SA and SB differ; inspect the parameter values.");
        exchangeMessage.setText("Public exchange complete ✓");
        exchangeBadge.setText("Exchange complete");
        style(exchangeBadge, "status-badge-ready");
        exchangeButton.setDisable(false);
        replayButton.setDisable(false);
        showStep(4);
        footerStatus.setText("COMPLETE  •  Both participants derived the same shared key");
    }

    private void invalidateResults() {
        stopAnimation();
        current = null;
        alicePublicFormula.setText("pa = G^PA mod P");
        bobPublicFormula.setText("pb = G^PB mod P");
        alicePublicValue.setText("—");
        bobPublicValue.setText("—");
        alicePrivateLabel.setText("?");
        bobPrivateLabel.setText("?");
        alicePublicLabel.setText("?");
        bobPublicLabel.setText("?");
        forwardArrow.setText("pa  ─────────▶");
        backArrow.setText("◀─────────  pb");
        aliceSecretFormula.setText("Waiting for the exchange…");
        bobSecretFormula.setText("Waiting for the exchange…");
        aliceSecretValue.setText("—");
        bobSecretValue.setText("—");
        matchBadge.setText("Pending");
        style(matchBadge, "status-badge-neutral");
        matchDetail.setText("SA and SB should match, even though PA and PB were never exchanged.");
        exchangeBadge.setText("Not started");
        style(exchangeBadge, "status-badge-neutral");
        exchangeMessage.setText("Public channel");
        animationHint.setManaged(true);
        animationHint.setVisible(true);
        animationHint.setText("Press Animate exchange to send pa → Bob and pb → Alice");
        exchangeButton.setDisable(true);
        replayButton.setDisable(true);
        validationBadge.setText("Awaiting input");
        style(validationBadge, "status-badge-neutral");
        style(inputFeedback, "validation-neutral");
        inputFeedback.setText("Enter your numbers, use Generate numbers, or load the textbook example.");
        showStep(1);
    }

    private void stopAnimation() {
        if (animation != null) {
            animation.stop();
            animation.setOnFinished(null);
            animation = null;
        }
        cleanupTokens();
    }

    private void cleanupTokens() {
        if (forwardToken != null) animationHost.getChildren().remove(forwardToken);
        if (backToken != null) animationHost.getChildren().remove(backToken);
        forwardToken = null;
        backToken = null;
    }

    private void showStep(int active) {
        Label[] steps = {step1, step2, step3, step4};
        for (int i = 0; i < steps.length; i++) {
            steps[i].getStyleClass().removeAll("step-pill", "step-pill-active", "step-pill-done");
            steps[i].getStyleClass().add(i + 1 == active ? "step-pill-active" : i + 1 < active ? "step-pill-done" : "step-pill");
        }
    }

    private void style(Label label, String cssClass) {
        label.getStyleClass().removeAll("status-badge-neutral", "status-badge-ready", "status-badge-teal", "validation-neutral", "validation-valid", "validation-invalid");
        label.getStyleClass().add(cssClass);
    }

    @FXML private void reset() {
        programmaticUpdate = true;
        try { pInput.clear(); gInput.clear(); aliceInput.clear(); bobInput.clear(); }
        finally { programmaticUpdate = false; }
        clearValidation();
        invalidateResults();
        footerStatus.setText("RESET COMPLETE  •  Ready for new parameters");
    }

    private void clearValidation() {
        invalidField = suggestedField = null;
        pVisited = gVisited = aliceVisited = bobVisited = false;
        suggestedNumber = 0;
        pInput.setDisable(false);
        gInput.setDisable(false);
        aliceInput.setDisable(false);
        bobInput.setDisable(false);
        computeButton.setDisable(false);
        suggestionButton.setVisible(false);
        suggestionButton.setManaged(false);
        for (TextField field : new TextField[]{pInput, gInput, aliceInput, bobInput}) {
            field.getStyleClass().remove("input-invalid");
        }
    }

    @FXML private void toggleTheme() {
        boolean dark = rootPane.getStyleClass().contains("dark-mode");
        if (dark) rootPane.getStyleClass().remove("dark-mode");
        else rootPane.getStyleClass().add("dark-mode");
        themeButton.setText(dark ? "◐  Dark" : "☼  Light");
    }

    private Stage stage() { return (Stage) rootPane.getScene().getWindow(); }
    @FXML private void minimize() { stage().setIconified(true); }
    @FXML private void maximize() {
        stage().setMaximized(!stage().isMaximized());
        maximizeWinBtn.setText(stage().isMaximized() ? "❐" : "▢");
    }
    @FXML private void close() { stage().close(); }
    @FXML private void titlePressed(MouseEvent event) {
        dragX = event.getSceneX(); dragY = event.getSceneY();
    }
    @FXML private void titleDragged(MouseEvent event) {
        Stage stage = stage();
        if (!stage.isMaximized()) {
            stage.setX(event.getScreenX() - dragX);
            stage.setY(event.getScreenY() - dragY);
        }
    }
    @FXML private void titleClicked(MouseEvent event) {
        if (event.getClickCount() == 2) maximize();
    }
}
