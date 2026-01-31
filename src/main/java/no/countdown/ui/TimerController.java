package no.countdown.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import no.countdown.model.CountdownTimer;
import no.countdown.model.CountdownTimer.TimeUnit;

import java.util.HashMap;
import java.util.Map;

public class TimerController extends BorderPane {

    private final ObservableList<CountdownTimer> timers = FXCollections.observableArrayList();
    private final Map<CountdownTimer, Timeline> timelines = new HashMap<>();
    private CountdownTimer selectedTimer;

    private final VBox timerListBox = new VBox(8);
    private final VBox centerDisplay = new VBox(16);
    private final VBox formContainer = new VBox();

    // Center display elements
    private final Label countdownLabel = new Label("00 : 00 : 00 : 00");
    private final Label unitLabelsRow = new Label("DAYS     HRS      MIN      SEC");
    private final Label centerThemeName = new Label();
    private final Label centerDescription = new Label();
    private final Label timesUpLabel = new Label("Time's Up!");
    private final Button pauseResumeBtn = new Button("Pause");
    private final Button resetBtn = new Button("Reset");
    private final HBox controlButtons = new HBox(12, pauseResumeBtn, resetBtn);

    private boolean formVisible = false;

    public TimerController() {
        getStyleClass().add("root-pane");
        setPadding(new Insets(0));

        setTop(createTopBar());
        setLeft(createLeftPanel());
        setCenter(createCenterPanel());

        setupControlButtons();

        timers.addListener((ListChangeListener<CountdownTimer>) c -> rebuildTimerList());
    }

    private HBox createTopBar() {
        Label title = new Label("Countdown Timer");
        title.getStyleClass().add("app-title");
        HBox topBar = new HBox(title);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(18, 24, 18, 24));
        return topBar;
    }

    private VBox createLeftPanel() {
        Label listTitle = new Label("Timers");
        listTitle.getStyleClass().add("list-title");

        timerListBox.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(timerListBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("timer-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addBtn = new Button("+ New Timer");
        addBtn.getStyleClass().add("add-timer-btn");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> toggleForm());

        formContainer.getStyleClass().add("form-container");
        formContainer.setManaged(false);
        formContainer.setVisible(false);

        VBox leftPanel = new VBox(10, listTitle, scrollPane, addBtn, formContainer);
        leftPanel.getStyleClass().add("left-panel");
        leftPanel.setPadding(new Insets(16));
        leftPanel.setPrefWidth(280);
        leftPanel.setMinWidth(280);

        return leftPanel;
    }

    private VBox createCenterPanel() {
        countdownLabel.getStyleClass().add("countdown-digits");
        unitLabelsRow.getStyleClass().add("unit-labels");

        centerThemeName.getStyleClass().add("center-theme-name");
        centerDescription.getStyleClass().add("center-description");

        timesUpLabel.getStyleClass().add("times-up-label");
        timesUpLabel.setVisible(false);
        timesUpLabel.setManaged(false);

        controlButtons.setAlignment(Pos.CENTER);
        controlButtons.setVisible(false);
        controlButtons.setManaged(false);

        Label emptyPrompt = new Label("Create a timer to get started");
        emptyPrompt.getStyleClass().add("empty-prompt");

        centerDisplay.setAlignment(Pos.CENTER);
        centerDisplay.setPadding(new Insets(40));
        centerDisplay.getChildren().addAll(
                centerThemeName, centerDescription,
                countdownLabel, unitLabelsRow,
                timesUpLabel, controlButtons, emptyPrompt
        );

        return centerDisplay;
    }

    private void setupControlButtons() {
        pauseResumeBtn.getStyleClass().add("control-btn");
        resetBtn.getStyleClass().add("control-btn");

        pauseResumeBtn.setOnAction(e -> {
            if (selectedTimer == null) return;
            if (selectedTimer.isRunning()) {
                selectedTimer.pause();
                pauseResumeBtn.setText("Resume");
            } else if (selectedTimer.isPaused()) {
                selectedTimer.resume();
                pauseResumeBtn.setText("Pause");
            }
        });

        resetBtn.setOnAction(e -> {
            if (selectedTimer == null) return;
            Timeline tl = timelines.get(selectedTimer);
            if (tl != null) tl.stop();
            selectedTimer.reset();
            updateCenterDisplay();
            rebuildTimerList();
        });
    }

    private void toggleForm() {
        formVisible = !formVisible;
        if (formVisible) {
            formContainer.getChildren().setAll(createForm());
            formContainer.setManaged(true);
            formContainer.setVisible(true);
        } else {
            formContainer.setManaged(false);
            formContainer.setVisible(false);
        }
    }

    private VBox createForm() {
        Label formTitle = new Label("New Timer");
        formTitle.getStyleClass().add("form-title");

        TextField nameField = new TextField();
        nameField.setPromptText("Timer name");
        nameField.getStyleClass().add("form-field");

        TextField descField = new TextField();
        descField.setPromptText("Description (optional)");
        descField.getStyleClass().add("form-field");

        Spinner<Integer> amountSpinner = new Spinner<>(1, 9999, 1);
        amountSpinner.setEditable(true);
        amountSpinner.getStyleClass().add("form-field");
        amountSpinner.setPrefWidth(Double.MAX_VALUE);

        ComboBox<TimeUnit> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll(TimeUnit.values());
        unitCombo.setValue(TimeUnit.MINUTES);
        unitCombo.getStyleClass().add("form-combo");
        unitCombo.setMaxWidth(Double.MAX_VALUE);

        HBox amountRow = new HBox(8, amountSpinner, unitCombo);
        HBox.setHgrow(amountSpinner, Priority.ALWAYS);
        HBox.setHgrow(unitCombo, Priority.ALWAYS);

        Button doneBtn = new Button("Done");
        doneBtn.getStyleClass().addAll("add-timer-btn", "done-btn");
        doneBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-btn");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        doneBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.setStyle("-fx-border-color: #ef4444;");
                return;
            }
            String desc = descField.getText().trim();
            int amount = amountSpinner.getValue();
            TimeUnit unit = unitCombo.getValue();

            CountdownTimer timer = new CountdownTimer(name, desc);
            timers.add(timer);

            // Start countdown immediately
            timer.startCountdown(amount, unit);
            startTimeline(timer);

            selectTimer(timer);
            toggleForm();
        });

        cancelBtn.setOnAction(e -> toggleForm());

        HBox btnRow = new HBox(8, doneBtn, cancelBtn);
        HBox.setHgrow(doneBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        VBox form = new VBox(8, formTitle, nameField, descField, amountRow, btnRow);
        form.setPadding(new Insets(12));
        form.getStyleClass().add("new-timer-form");
        return form;
    }

    private void startTimeline(CountdownTimer timer) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timer.updateRemaining();
            if (timer == selectedTimer) {
                updateCenterDisplay();
            }
            rebuildTimerList();
            if (timer.isFinished()) {
                Timeline tl = timelines.get(timer);
                if (tl != null) tl.stop();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        timelines.put(timer, timeline);
    }

    private void selectTimer(CountdownTimer timer) {
        selectedTimer = timer;
        updateCenterDisplay();
        rebuildTimerList();
    }

    private void updateCenterDisplay() {
        // Find and hide the empty prompt
        centerDisplay.getChildren().stream()
                .filter(n -> n.getStyleClass().contains("empty-prompt"))
                .forEach(n -> { n.setVisible(selectedTimer == null); n.setManaged(selectedTimer == null); });

        if (selectedTimer == null) {
            countdownLabel.setVisible(false);
            countdownLabel.setManaged(false);
            unitLabelsRow.setVisible(false);
            unitLabelsRow.setManaged(false);
            centerThemeName.setVisible(false);
            centerThemeName.setManaged(false);
            centerDescription.setVisible(false);
            centerDescription.setManaged(false);
            timesUpLabel.setVisible(false);
            timesUpLabel.setManaged(false);
            controlButtons.setVisible(false);
            controlButtons.setManaged(false);
            return;
        }

        centerThemeName.setText(selectedTimer.getThemeName());
        centerThemeName.setVisible(true);
        centerThemeName.setManaged(true);

        String desc = selectedTimer.getDescription();
        centerDescription.setText(desc);
        centerDescription.setVisible(!desc.isEmpty());
        centerDescription.setManaged(!desc.isEmpty());

        countdownLabel.setVisible(true);
        countdownLabel.setManaged(true);
        unitLabelsRow.setVisible(true);
        unitLabelsRow.setManaged(true);

        long total = selectedTimer.getRemainingSeconds();
        long days = total / 86400;
        long hours = (total % 86400) / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        countdownLabel.setText(String.format("%02d : %02d : %02d : %02d", days, hours, minutes, seconds));

        boolean isFinished = selectedTimer.isFinished();
        timesUpLabel.setVisible(isFinished);
        timesUpLabel.setManaged(isFinished);

        boolean showControls = !isFinished && (selectedTimer.isRunning() || selectedTimer.isPaused());
        controlButtons.setVisible(showControls || isFinished);
        controlButtons.setManaged(showControls || isFinished);

        if (selectedTimer.isRunning()) {
            pauseResumeBtn.setText("Pause");
            pauseResumeBtn.setVisible(true);
        } else if (selectedTimer.isPaused()) {
            pauseResumeBtn.setText("Resume");
            pauseResumeBtn.setVisible(true);
        } else {
            pauseResumeBtn.setVisible(!isFinished);
        }
    }

    private void rebuildTimerList() {
        timerListBox.getChildren().clear();
        for (int i = 0; i < timers.size(); i++) {
            CountdownTimer timer = timers.get(i);
            timerListBox.getChildren().add(createTimerCard(timer, i));
        }
    }

    private HBox createTimerCard(CountdownTimer timer, int index) {
        VBox info = new VBox(4);
        Label nameLabel = new Label(timer.getThemeName());
        nameLabel.getStyleClass().add("card-name");

        String timeText;
        if (timer.isFinished()) {
            timeText = "Time's Up!";
        } else if (timer.isRunning() || timer.isPaused()) {
            long total = timer.getRemainingSeconds();
            long d = total / 86400;
            long h = (total % 86400) / 3600;
            long m = (total % 3600) / 60;
            long s = total % 60;
            if (d > 0) {
                timeText = String.format("%dd %dh %dm %ds", d, h, m, s);
            } else if (h > 0) {
                timeText = String.format("%dh %dm %ds", h, m, s);
            } else {
                timeText = String.format("%dm %ds", m, s);
            }
            if (timer.isPaused()) timeText += " (paused)";
        } else {
            timeText = "Not started";
        }

        Label timeLabel = new Label(timeText);
        timeLabel.getStyleClass().add("card-time");

        info.getChildren().addAll(nameLabel, timeLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Reorder buttons
        Button upBtn = new Button("\u25B2");
        upBtn.getStyleClass().add("arrow-btn");
        upBtn.setDisable(index == 0);
        upBtn.setOnAction(e -> {
            if (index > 0) {
                CountdownTimer t = timers.remove(index);
                timers.add(index - 1, t);
            }
        });

        Button downBtn = new Button("\u25BC");
        downBtn.getStyleClass().add("arrow-btn");
        downBtn.setDisable(index == timers.size() - 1);
        downBtn.setOnAction(e -> {
            if (index < timers.size() - 1) {
                CountdownTimer t = timers.remove(index);
                timers.add(index + 1, t);
            }
        });

        VBox arrows = new VBox(2, upBtn, downBtn);
        arrows.setAlignment(Pos.CENTER);

        HBox card = new HBox(10, info, arrows);
        card.getStyleClass().add("timer-card");
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setAlignment(Pos.CENTER_LEFT);

        if (timer == selectedTimer) {
            card.getStyleClass().add("timer-card-selected");
        }

        card.setOnMouseClicked(e -> selectTimer(timer));

        return card;
    }
}
