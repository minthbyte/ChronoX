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

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimerController extends BorderPane {

    private static final DataFormat TIMER_INDEX = new DataFormat("application/x-timer-index");
    private static final Path SAVE_DIR = Paths.get(System.getProperty("user.home"), ".chronox");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("timers.txt");

    private final ObservableList<CountdownTimer> timers = FXCollections.observableArrayList();
    private final Map<CountdownTimer, Timeline> timelines = new HashMap<>();
    private final Map<CountdownTimer, CardControls> cardControls = new HashMap<>();
    private CountdownTimer selectedTimer;
    private int dragSourceIndex = -1;

    private static class CardControls {
        Label timeLabel;
        ProgressBar progressBar;
        Button cardDoneBtn;
    }

    private final VBox timerListBox = new VBox(8);
    private final VBox centerDisplay = new VBox(16);
    private final VBox formContainer = new VBox();
    private final HBox filterBar = new HBox(8);
    private final TextField searchField = new TextField();
    private String classFilter = null;
    private String searchQuery = "";

    // Center display elements
    private final Label countdownLabel = new Label("00 : 00 : 00 : 00");
    private final Label unitLabelsRow = new Label("DAYS     HRS      MIN      SEC");
    private final Label centerThemeName = new Label();
    private final Label centerDescription = new Label();
    private final Label timesUpLabel = new Label("Time's Up!");
    private final Button pauseResumeBtn = new Button("Pause");
    private final Button resetBtn = new Button("Reset");
    private final Button cancelTimerBtn = new Button("Cancel");
    private final Button deleteBtn = new Button("Delete");
    private final Button restartDoneBtn = new Button("Done");
    private final HBox controlButtons = new HBox(12, pauseResumeBtn, resetBtn, cancelTimerBtn);
    private final HBox finishedButtons = new HBox(12, restartDoneBtn);
    private VBox guideBox;

    private boolean formVisible = false;
    private CountdownTimer editingTimer = null;

    public TimerController() {
        getStyleClass().add("root-pane");
        setPadding(new Insets(0));

        setTop(createTopBar());
        setLeft(createLeftPanel());
        setCenter(createCenterPanel());

        setupControlButtons();

        timers.addListener((ListChangeListener<CountdownTimer>) c -> {
            rebuildTimerList();
            saveTimers();
        });

        loadTimers();
    }

    private HBox createTopBar() {
        Label title = new Label("ChronoX");
        title.getStyleClass().add("app-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("save-btn");
        saveBtn.setOnAction(e -> {
            saveTimers();
            saveBtn.setText("Saved!");
            Timeline revert = new Timeline(new KeyFrame(Duration.seconds(1.5), ev -> saveBtn.setText("Save")));
            revert.play();
        });

        HBox topBar = new HBox(title, spacer, saveBtn);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(18, 24, 18, 24));
        return topBar;
    }

    private VBox createLeftPanel() {
        Label listTitle = new Label("Timers");
        listTitle.getStyleClass().add("list-title");

        timerListBox.setPadding(new Insets(4));

        searchField.setPromptText("Search timers...");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, old, val) -> {
            searchQuery = val == null ? "" : val.trim().toLowerCase();
            rebuildTimerList();
        });

        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");
        filterBar.setVisible(false);
        filterBar.setManaged(false);

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

        VBox leftPanel = new VBox(10, listTitle, searchField, filterBar, scrollPane, addBtn, formContainer);
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
            "2.  Enter name, description, class, amount and unit\n" +
            "3.  Click \"Done\" \u2014 countdown starts immediately\n" +
            "4.  Click a timer card to view the full countdown\n" +
            "5.  Use Pause, Resume, Reset, Edit or Delete\n" +
            "6.  Right-click a card for a quick context menu\n" +
            "7.  Click a class tag to filter the list\n" +
            "8.  Drag the \u2630 handle to reorder timers\n" +
            "9.  Finished timers float to the top automatically\n" +
            "10. Timers are saved automatically on exit"
        );
        guideText.getStyleClass().add("guide-text");

        guideBox = new VBox(12, guideTitle, guideText);
        guideBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("edit-btn");
        editBtn.setOnAction(e -> {
            if (selectedTimer != null) openEditForm(selectedTimer);
        });

        HBox deleteRow = new HBox(12, editBtn, deleteBtn);
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
            saveTimers();
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

        cancelTimerBtn.getStyleClass().add("cancel-btn");
        cancelTimerBtn.setOnAction(e -> {
            if (selectedTimer == null) return;
            Timeline tl = timelines.remove(selectedTimer);
            if (tl != null) tl.stop();
            selectedTimer.reset();
            updateCenterDisplay();
            rebuildTimerList();
            saveTimers();
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
        editingTimer = null;
        if (formVisible) {
            formContainer.getChildren().setAll(createForm(null));
            formContainer.setManaged(true);
            formContainer.setVisible(true);
        } else {
            formContainer.setManaged(false);
            formContainer.setVisible(false);
        }
    }

    private void openEditForm(CountdownTimer timer) {
        editingTimer = timer;
        formVisible = true;
        formContainer.getChildren().setAll(createForm(timer));
        formContainer.setManaged(true);
        formContainer.setVisible(true);
    }

    private void closeForm() {
        formVisible = false;
        editingTimer = null;
        formContainer.setManaged(false);
        formContainer.setVisible(false);
    }

    private VBox createForm(CountdownTimer editing) {
        boolean isEdit = editing != null;

        Label formTitle = new Label(isEdit ? "Edit Timer" : "New Timer");
        formTitle.getStyleClass().add("form-title");

        TextField nameField = new TextField();
        nameField.setPromptText("Timer name");
        nameField.getStyleClass().add("form-field");

        TextField descField = new TextField();
        descField.setPromptText("Description (optional)");
        descField.getStyleClass().add("form-field");

        Label classLabel = new Label("Class (optional)");
        classLabel.getStyleClass().add("form-label");

        ComboBox<String> classCombo = new ComboBox<>();
        classCombo.setEditable(true);
        classCombo.getStyleClass().add("form-combo");
        classCombo.setMaxWidth(Double.MAX_VALUE);
        classCombo.setPromptText("Select or type a class");
        List<String> existingClasses = timers.stream()
                .map(CountdownTimer::getClassName)
                .filter(cn -> cn != null && !cn.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        classCombo.getItems().addAll(existingClasses);

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

        if (isEdit) {
            nameField.setText(editing.getThemeName());
            descField.setText(editing.getDescription());
            String cn = editing.getClassName();
            if (cn != null && !cn.isEmpty()) {
                classCombo.getEditor().setText(cn);
            }
            if (editing.getOriginalUnit() != null) {
                amountSpinner.getValueFactory().setValue((int) editing.getOriginalAmount());
                unitCombo.setValue(editing.getOriginalUnit());
            }
        }

        Button doneBtn = new Button(isEdit ? "Save" : "Done");
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
            String cls = classCombo.getEditor().getText() != null ? classCombo.getEditor().getText().trim() : "";

            if (isEdit) {
                editing.setThemeName(name);
                editing.setDescription(desc);
                editing.setClassName(cls);

                int newAmount = amountSpinner.getValue();
                TimeUnit newUnit = unitCombo.getValue();
                if (newAmount != editing.getOriginalAmount() || newUnit != editing.getOriginalUnit()) {
                    Timeline oldTl = timelines.remove(editing);
                    if (oldTl != null) oldTl.stop();
                    editing.startCountdown(newAmount, newUnit);
                    startTimeline(editing);
                }

                updateCenterDisplay();
                rebuildTimerList();
                saveTimers();
                closeForm();
            } else {
                int amount = amountSpinner.getValue();
                TimeUnit unit = unitCombo.getValue();

                CountdownTimer timer = new CountdownTimer(name, desc, cls);
                timers.add(timer);
                timer.startCountdown(amount, unit);
                startTimeline(timer);
                selectTimer(timer);
                closeForm();
            }
        });

        cancelBtn.setOnAction(e -> closeForm());

        HBox btnRow = new HBox(8, doneBtn, cancelBtn);
        HBox.setHgrow(doneBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);

        VBox form = new VBox(8, formTitle, nameField, descField, classLabel, classCombo, amountLabel, amountSpinner, unitLabel, unitCombo, btnRow);
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
            if (timer.isFinished()) {
                Timeline tl = timelines.get(timer);
                if (tl != null) tl.stop();
                java.awt.Toolkit.getDefaultToolkit().beep();
                saveTimers();
                rebuildTimerList(); // structural change: finished moves to top
            } else {
                refreshCards();
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
        if (dragSourceIndex != -1) return; // don't rebuild while dragging

        cardControls.clear();
        timerListBox.getChildren().clear();
        // Finished timers first, then by class name, then original order
        List<CountdownTimer> sorted = timers.stream()
                .filter(t -> {
                    if (classFilter != null && !classFilter.equals(t.getClassName())) return false;
                    if (!searchQuery.isEmpty()) {
                        String name = t.getThemeName() != null ? t.getThemeName().toLowerCase() : "";
                        String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
                        String cls = t.getClassName() != null ? t.getClassName().toLowerCase() : "";
                        return name.contains(searchQuery) || desc.contains(searchQuery) || cls.contains(searchQuery);
                    }
                    return true;
                })
                .sorted(Comparator
                        .comparing((CountdownTimer t) -> !t.isFinished())
                        .thenComparing(t -> {
                            String cn = t.getClassName();
                            return (cn == null || cn.isEmpty()) ? "" : cn;
                        })
                        .thenComparing(t -> timers.indexOf(t)))
                .collect(Collectors.toList());
        for (int i = 0; i < sorted.size(); i++) {
            timerListBox.getChildren().add(createTimerCard(sorted.get(i), i, sorted));
        }
    }

    private void refreshCards() {
        for (var entry : cardControls.entrySet()) {
            CountdownTimer t = entry.getKey();
            CardControls cc = entry.getValue();

            String timeText;
            if (t.isFinished()) {
                timeText = "Time's Up!";
            } else if (t.isRunning() || t.isPaused()) {
                long total = t.getRemainingSeconds();
                long d = total / 86400, h = (total % 86400) / 3600, m = (total % 3600) / 60, s = total % 60;
                if (d > 0) timeText = String.format("%dd %dh %dm %ds", d, h, m, s);
                else if (h > 0) timeText = String.format("%dh %dm %ds", h, m, s);
                else timeText = String.format("%dm %ds", m, s);
                if (t.isPaused()) timeText += " (paused)";
            } else {
                timeText = "Not started";
            }
            cc.timeLabel.setText(timeText);

            if (t.isFinished()) cc.progressBar.setProgress(1.0);
            else if (t.isRunning() || t.isPaused()) cc.progressBar.setProgress(t.getProgress());
            else cc.progressBar.setProgress(0);

            boolean stopped = !t.isRunning() && !t.isPaused() && t.getOriginalUnit() != null;
            cc.cardDoneBtn.setVisible(stopped);
            cc.cardDoneBtn.setManaged(stopped);
        }
    }

    private HBox createTimerCard(CountdownTimer timer, int displayIndex, List<CountdownTimer> displayList) {
        Label nameLabel = new Label(timer.getThemeName());
        nameLabel.getStyleClass().add("card-name");

        HBox nameRow = new HBox(6, nameLabel);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        String cn = timer.getClassName();
        if (cn != null && !cn.isEmpty()) {
            Label classTag = new Label(cn);
            classTag.getStyleClass().add("class-tag");
            String tagColor = classColor(cn);
            classTag.setStyle("-fx-background-color: " + tagColor + "33; -fx-text-fill: " + tagColor + ";");
            classTag.setOnMouseClicked(e -> {
                setClassFilter(cn);
                e.consume();
            });
            nameRow.getChildren().add(classTag);
        }

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

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("card-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        if (timer.isFinished()) {
            progressBar.setProgress(1.0);
        } else if (timer.isRunning() || timer.isPaused()) {
            progressBar.setProgress(timer.getProgress());
        } else {
            progressBar.setProgress(0);
        }
        progressBar.setVisible(timer.getTotalSeconds() > 0);
        progressBar.setManaged(timer.getTotalSeconds() > 0);

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

        CardControls cc = new CardControls();
        cc.timeLabel = timeLabel;
        cc.progressBar = progressBar;
        cc.cardDoneBtn = cardDoneBtn;
        cardControls.put(timer, cc);

        VBox info = new VBox(4, nameRow, progressBar, timeLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        cardDoneBtn.setMaxWidth(Double.MAX_VALUE);

        HBox cardRow = new HBox(10, dragHandle, info);
        cardRow.setAlignment(Pos.CENTER_LEFT);
        HBox card = new HBox();
        VBox cardWrapper = new VBox(6, cardRow, cardDoneBtn);
        card.getChildren().add(cardWrapper);
        HBox.setHgrow(cardWrapper, Priority.ALWAYS);
        card.getStyleClass().add("timer-card");
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setAlignment(Pos.CENTER_LEFT);

        if (timer == selectedTimer) {
            card.getStyleClass().add("timer-card-selected");
        }

        card.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                selectTimer(timer);
            }
        });

        // Context menu (right-click)
        ContextMenu ctx = new ContextMenu();

        if (timer.isRunning()) {
            MenuItem pauseItem = new MenuItem("Pause");
            pauseItem.setOnAction(e -> {
                timer.pause();
                if (timer == selectedTimer) updateCenterDisplay();
                rebuildTimerList();
                saveTimers();
            });
            ctx.getItems().add(pauseItem);
        } else if (timer.isPaused()) {
            MenuItem resumeItem = new MenuItem("Resume");
            resumeItem.setOnAction(e -> {
                timer.resume();
                if (timer == selectedTimer) updateCenterDisplay();
                rebuildTimerList();
                saveTimers();
            });
            ctx.getItems().add(resumeItem);
        }

        if (timer.isFinished() || (!timer.isRunning() && !timer.isPaused() && timer.getOriginalUnit() != null)) {
            MenuItem restartItem = new MenuItem("Restart");
            restartItem.setOnAction(e -> {
                Timeline tl = timelines.remove(timer);
                if (tl != null) tl.stop();
                timer.restart();
                startTimeline(timer);
                if (timer == selectedTimer) updateCenterDisplay();
                rebuildTimerList();
            });
            ctx.getItems().add(restartItem);
        }

        if (timer.isRunning() || timer.isPaused()) {
            MenuItem resetItem = new MenuItem("Reset");
            resetItem.setOnAction(e -> {
                Timeline tl = timelines.remove(timer);
                if (tl != null) tl.stop();
                timer.restart();
                startTimeline(timer);
                if (timer == selectedTimer) updateCenterDisplay();
                rebuildTimerList();
            });
            ctx.getItems().add(resetItem);
        }

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            selectTimer(timer);
            openEditForm(timer);
        });
        ctx.getItems().add(editItem);

        ctx.getItems().add(new SeparatorMenuItem());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            Timeline tl = timelines.remove(timer);
            if (tl != null) tl.stop();
            timers.remove(timer);
            if (timer == selectedTimer) {
                selectedTimer = null;
                updateCenterDisplay();
            }
            rebuildTimerList();
        });
        ctx.getItems().add(deleteItem);

        card.setOnContextMenuRequested(e -> {
            ctx.show(card, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // Drag-and-drop reordering
        card.setOnDragDetected(e -> {
            dragSourceIndex = displayIndex;
            var db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent clip = new ClipboardContent();
            clip.put(TIMER_INDEX, displayIndex);
            db.setContent(clip);
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
                int fromDisplay = (int) db.getContent(TIMER_INDEX);
                int toDisplay = displayIndex;
                if (fromDisplay != toDisplay && fromDisplay < displayList.size() && toDisplay < displayList.size()) {
                    CountdownTimer movedTimer = displayList.get(fromDisplay);
                    CountdownTimer targetTimer = displayList.get(toDisplay);
                    int fromBacking = timers.indexOf(movedTimer);
                    int toBacking = timers.indexOf(targetTimer);
                    if (fromBacking != -1 && toBacking != -1 && fromBacking != toBacking) {
                        timers.remove(fromBacking);
                        // recalculate target position after removal
                        toBacking = timers.indexOf(targetTimer);
                        timers.add(toBacking, movedTimer);
                    }
                }
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        card.setOnDragDone(e -> {
            dragSourceIndex = -1;
            rebuildTimerList();
            e.consume();
        });

        return card;
    }

    private void setClassFilter(String cn) {
        classFilter = cn;
        filterBar.getChildren().clear();

        Label filterLabel = new Label("Class:");
        filterLabel.getStyleClass().add("filter-label");

        Label filterTag = new Label(cn);
        filterTag.getStyleClass().add("class-tag");
        String tagColor = classColor(cn);
        filterTag.setStyle("-fx-background-color: " + tagColor + "33; -fx-text-fill: " + tagColor + ";");

        Button clearBtn = new Button("\u2715");
        clearBtn.getStyleClass().add("filter-clear-btn");
        clearBtn.setOnAction(e -> clearClassFilter());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterBar.getChildren().addAll(filterLabel, filterTag, spacer, clearBtn);
        filterBar.setVisible(true);
        filterBar.setManaged(true);

        rebuildTimerList();
    }

    private void clearClassFilter() {
        classFilter = null;
        filterBar.setVisible(false);
        filterBar.setManaged(false);
        rebuildTimerList();
    }

    // --- Persistence ---

    public void saveTimers() {
        try {
            Files.createDirectories(SAVE_DIR);
            List<String> lines = new ArrayList<>();
            for (CountdownTimer t : timers) {
                lines.add("name=" + t.getThemeName());
                lines.add("description=" + t.getDescription());
                lines.add("className=" + t.getClassName());
                lines.add("originalAmount=" + t.getOriginalAmount());
                lines.add("originalUnit=" + (t.getOriginalUnit() != null ? t.getOriginalUnit().name() : ""));
                lines.add("totalSeconds=" + t.getTotalSeconds());
                if (t.isRunning() && t.targetTimeProperty().get() != null) {
                    lines.add("state=running");
                    lines.add("targetTime=" + t.targetTimeProperty().get().toString());
                } else if (t.isPaused()) {
                    lines.add("state=paused");
                    lines.add("remainingSeconds=" + t.getRemainingSeconds());
                } else if (t.isFinished()) {
                    lines.add("state=finished");
                } else {
                    lines.add("state=idle");
                }
                lines.add("---");
            }
            Files.write(SAVE_FILE, lines);
            System.out.println("[ChronoX] Saved " + timers.size() + " timer(s) to " + SAVE_FILE);
        } catch (IOException ex) {
            System.err.println("[ChronoX] Failed to save: " + ex.getMessage());
        }
    }

    private void loadTimers() {
        if (!Files.exists(SAVE_FILE)) {
            System.out.println("[ChronoX] No save file found at " + SAVE_FILE);
            return;
        }
        try {
            List<String> lines = Files.readAllLines(SAVE_FILE);
            System.out.println("[ChronoX] Loading from " + SAVE_FILE + " (" + lines.size() + " lines)");
            Map<String, String> block = new HashMap<>();
            for (String line : lines) {
                if (line.equals("---")) {
                    if (!block.isEmpty()) {
                        restoreTimer(block);
                        block.clear();
                    }
                } else {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        block.put(line.substring(0, eq), line.substring(eq + 1));
                    }
                }
            }
            if (!block.isEmpty()) restoreTimer(block);
        } catch (IOException ignored) {
        }
    }

    private void restoreTimer(Map<String, String> data) {
        String name = data.getOrDefault("name", "");
        String desc = data.getOrDefault("description", "");
        String cls = data.getOrDefault("className", "");
        String unitStr = data.getOrDefault("originalUnit", "");
        String state = data.getOrDefault("state", "idle");
        long amount = 0;
        long totalSec = 0;
        try { amount = Long.parseLong(data.getOrDefault("originalAmount", "0")); } catch (NumberFormatException ignored) {}
        try { totalSec = Long.parseLong(data.getOrDefault("totalSeconds", "0")); } catch (NumberFormatException ignored) {}

        if (name.isEmpty() || unitStr.isEmpty()) return;

        CountdownTimer.TimeUnit unit;
        try { unit = CountdownTimer.TimeUnit.valueOf(unitStr); } catch (IllegalArgumentException e) { return; }

        CountdownTimer timer = new CountdownTimer(name, desc, cls);

        switch (state) {
            case "running" -> {
                String targetStr = data.getOrDefault("targetTime", "");
                if (!targetStr.isEmpty()) {
                    LocalDateTime target = LocalDateTime.parse(targetStr);
                    timer.restoreRunning(amount, unit, totalSec, target);
                    timers.add(timer);
                    startTimeline(timer);
                } else {
                    return;
                }
            }
            case "paused" -> {
                long remaining = 0;
                try { remaining = Long.parseLong(data.getOrDefault("remainingSeconds", "0")); } catch (NumberFormatException ignored) {}
                timer.restorePaused(amount, unit, totalSec, remaining);
                timers.add(timer);
            }
            case "finished" -> {
                timer.restoreFinished(amount, unit, totalSec);
                timers.add(timer);
            }
            default -> {
                // idle — just store the original settings, don't start
                timer.restoreFinished(amount, unit, totalSec);
                timers.add(timer);
            }
        }
    }

    public void shutdown() {
        saveTimers();
        timelines.values().forEach(Timeline::stop);
    }

    private String classColor(String className) {
        int hash = className.hashCode();
        double hue = Math.abs(hash % 360);
        double sat = 0.65 + (Math.abs((hash >> 8) % 20)) / 100.0;
        double bri = 0.75 + (Math.abs((hash >> 16) % 15)) / 100.0;
        javafx.scene.paint.Color color = javafx.scene.paint.Color.hsb(hue, sat, bri);
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }
}
