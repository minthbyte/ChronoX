package no.countdown.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import no.countdown.model.CountdownTimer;
import no.countdown.model.CountdownTimer.TimeUnit;

import java.util.HashMap;
import java.util.Map;

public class TimerController extends BorderPane {

    private static final DataFormat TIMER_INDEX = new DataFormat("application/x-timer-index");

    private final ObservableList<CountdownTimer> timers = FXCollections.observableArrayList();
    private final Map<CountdownTimer, Timeline> timelines = new HashMap<>();
    private CountdownTimer selectedTimer;
    private int dragSourceIndex = -1;

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
    private final Button deleteBtn = new Button("Delete");
    private final Button restartDoneBtn = new Button("Done");
    private final HBox controlButtons = new HBox(12, pauseResumeBtn, resetBtn);
    private final HBox finishedButtons = new HBox(12, restartDoneBtn);
    private VBox guideBox;

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
        Label title = new Label("ChronoX");
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

        finishedButtons.setAlignment(Pos.CENTER);
        finishedButtons.setVisible(false);
        finishedButtons.setManaged(false);

        Label guideTitle = new Label("Welcome to ChronoX");
        guideTitle.getStyleClass().add("guide-title");

        Label guideText = new Label(
            "1.  Click \"+ New Timer\" to create a timer\n" +
            "2.  Enter a name, amount and time unit\n" +
            "3.  Click \"Done\" \u2014 countdown starts immediately\n" +
            "4.  Click a timer to view the full countdown\n" +
            "5.  Use Pause, Reset or Delete to control it\n" +
            "6.  Drag the \u2630 handle to reorder timers\n" +
            "7.  When finished, press \"Done\" to restart"
        );
        guideText.getStyleClass().add("guide-text");

        guideBox = new VBox(12, guideTitle, guideText);
        guideBox.setAlignment(Pos.CENTER);

        HBox deleteRow = new HBox(deleteBtn);
        deleteRow.setAlignment(Pos.CENTER);
        deleteRow.setVisible(false);
        deleteRow.setManaged(false);

        centerDisplay.setAlignment(Pos.CENTER);
        centerDisplay.setPadding(new Insets(40));
        centerDisplay.getChildren().addAll(
                centerThemeName, centerDescription,
                countdownLabel, unitLabelsRow,
                timesUpLabel, controlButtons, finishedButtons, deleteRow, guideBox
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
            Timeline tl = timelines.remove(selectedTimer);
            if (tl != null) tl.stop();
            selectedTimer.restart();
            startTimeline(selectedTimer);
            updateCenterDisplay();
            rebuildTimerList();
        });

        restartDoneBtn.getStyleClass().addAll("add-timer-btn", "done-btn");
        restartDoneBtn.setOnAction(e -> {
            if (selectedTimer == null || selectedTimer.isRunning()) return;
            Timeline tl = timelines.remove(selectedTimer);
            if (tl != null) tl.stop();
            selectedTimer.restart();
            startTimeline(selectedTimer);
            updateCenterDisplay();
            rebuildTimerList();
        });

        deleteBtn.getStyleClass().add("delete-btn");
        deleteBtn.setOnAction(e -> {
            if (selectedTimer == null) return;
            Timeline tl = timelines.remove(selectedTimer);
            if (tl != null) tl.stop();
            timers.remove(selectedTimer);
            selectedTimer = null;
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

        Label amountLabel = new Label("Amount");
        amountLabel.getStyleClass().add("form-label");

        Spinner<Integer> amountSpinner = new Spinner<>(1, 9999, 1);
        amountSpinner.setEditable(true);
        amountSpinner.getStyleClass().add("form-field");
        amountSpinner.setMaxWidth(Double.MAX_VALUE);

        Label unitLabel = new Label("Unit");
        unitLabel.getStyleClass().add("form-label");

        ComboBox<TimeUnit> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll(TimeUnit.values());
        unitCombo.setValue(TimeUnit.MINUTES);
        unitCombo.getStyleClass().add("form-combo");
        unitCombo.setMaxWidth(Double.MAX_VALUE);

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

        VBox form = new VBox(8, formTitle, nameField, descField, amountLabel, amountSpinner, unitLabel, unitCombo, btnRow);
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
            finishedButtons.setVisible(false);
            finishedButtons.setManaged(false);
            deleteBtn.getParent().setVisible(false);
            ((HBox) deleteBtn.getParent()).setManaged(false);
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
        controlButtons.setVisible(showControls);
        controlButtons.setManaged(showControls);

        boolean isStopped = !selectedTimer.isRunning() && !selectedTimer.isPaused();
        finishedButtons.setVisible(isStopped);
        finishedButtons.setManaged(isStopped);

        if (selectedTimer.isRunning()) {
            pauseResumeBtn.setText("Pause");
        } else if (selectedTimer.isPaused()) {
            pauseResumeBtn.setText("Resume");
        }

        deleteBtn.getParent().setVisible(true);
        ((HBox) deleteBtn.getParent()).setManaged(true);
    }

    private void rebuildTimerList() {
        timerListBox.getChildren().clear();
        for (int i = 0; i < timers.size(); i++) {
            CountdownTimer timer = timers.get(i);
            timerListBox.getChildren().add(createTimerCard(timer, i));
        }
    }

    private HBox createTimerCard(CountdownTimer timer, int index) {
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

        Label dragHandle = new Label("\u2630");
        dragHandle.getStyleClass().add("drag-handle");

        Button cardDoneBtn = new Button("Done");
        cardDoneBtn.getStyleClass().add("card-done-btn");
        boolean isStopped = !timer.isRunning() && !timer.isPaused() && timer.getOriginalUnit() != null;
        cardDoneBtn.setVisible(isStopped);
        cardDoneBtn.setManaged(isStopped);
        cardDoneBtn.setOnAction(e -> {
            Timeline tl = timelines.remove(timer);
            if (tl != null) tl.stop();
            timer.restart();
            startTimeline(timer);
            if (timer == selectedTimer) updateCenterDisplay();
            rebuildTimerList();
        });

        VBox info = new VBox(4, nameLabel, timeLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox card = new HBox(10, dragHandle, info, cardDoneBtn);
        card.getStyleClass().add("timer-card");
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setAlignment(Pos.CENTER_LEFT);

        if (timer == selectedTimer) {
            card.getStyleClass().add("timer-card-selected");
        }

        card.setOnMouseClicked(e -> selectTimer(timer));

        // Drag-and-drop reordering
        card.setOnDragDetected(e -> {
            dragSourceIndex = index;
            var db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.put(TIMER_INDEX, index);
            db.setContent(cc);
            card.getStyleClass().add("timer-card-dragging");
            e.consume();
        });

        card.setOnDragOver(e -> {
            if (e.getGestureSource() != card && e.getDragboard().hasContent(TIMER_INDEX)) {
                e.acceptTransferModes(TransferMode.MOVE);
                card.getStyleClass().add("timer-card-drop-target");
            }
            e.consume();
        });

        card.setOnDragExited(e -> {
            card.getStyleClass().remove("timer-card-drop-target");
            e.consume();
        });

        card.setOnDragDropped(e -> {
            var db = e.getDragboard();
            if (db.hasContent(TIMER_INDEX)) {
                int from = (int) db.getContent(TIMER_INDEX);
                int to = index;
                if (from != to) {
                    CountdownTimer moved = timers.remove(from);
                    timers.add(to, moved);
                }
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        card.setOnDragDone(e -> {
            dragSourceIndex = -1;
            e.consume();
        });

        return card;
    }
}
