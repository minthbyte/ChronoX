package no.countdown.model;

import javafx.beans.property.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class CountdownTimer {

    public enum TimeUnit {
        MINUTES, HOURS, DAYS, WEEKS, MONTHS;

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    private final StringProperty themeName = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty className = new SimpleStringProperty("");
    private final ObjectProperty<LocalDateTime> targetTime = new SimpleObjectProperty<>();
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty finished = new SimpleBooleanProperty(false);
    private final LongProperty remainingSeconds = new SimpleLongProperty(0);

    private Duration pausedDuration;
    private long originalAmount;
    private TimeUnit originalUnit;
    private long totalSeconds;

    public CountdownTimer(String themeName, String description) {
        this.themeName.set(themeName);
        this.description.set(description);
    }

    public CountdownTimer(String themeName, String description, String className) {
        this.themeName.set(themeName);
        this.description.set(description);
        this.className.set(className != null ? className : "");
    }

    public void startCountdown(long amount, TimeUnit unit) {
        this.originalAmount = amount;
        this.originalUnit = unit;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = switch (unit) {
            case MINUTES -> now.plusMinutes(amount);
            case HOURS -> now.plusHours(amount);
            case DAYS -> now.plusDays(amount);
            case WEEKS -> now.plusWeeks(amount);
            case MONTHS -> now.plusMonths(amount);
        };
        targetTime.set(target);
        totalSeconds = Duration.between(now, target).toSeconds();
        finished.set(false);
        running.set(true);
        pausedDuration = null;
        updateRemaining();
    }

    public void updateRemaining() {
        if (!running.get() || finished.get()) return;

        LocalDateTime target = targetTime.get();
        if (target == null) return;

        Duration dur = Duration.between(LocalDateTime.now(), target);
        if (dur.isNegative() || dur.isZero()) {
            remainingSeconds.set(0);
            running.set(false);
            finished.set(true);
        } else {
            remainingSeconds.set(dur.toSeconds());
        }
    }

    public void pause() {
        if (!running.get() || finished.get()) return;
        LocalDateTime target = targetTime.get();
        if (target == null) return;

        pausedDuration = Duration.between(LocalDateTime.now(), target);
        if (pausedDuration.isNegative()) pausedDuration = Duration.ZERO;
        running.set(false);
    }

    public void resume() {
        if (running.get() || finished.get() || pausedDuration == null) return;

        targetTime.set(LocalDateTime.now().plus(pausedDuration));
        pausedDuration = null;
        running.set(true);
    }

    public void reset() {
        running.set(false);
        finished.set(false);
        remainingSeconds.set(0);
        targetTime.set(null);
        pausedDuration = null;
    }

    public void restart() {
        if (originalUnit == null) return;
        startCountdown(originalAmount, originalUnit);
    }

    public boolean isPaused() {
        return !running.get() && !finished.get() && pausedDuration != null;
    }

    /** Restore a running timer from saved targetTime */
    public void restoreRunning(long amount, TimeUnit unit, long savedTotalSeconds, LocalDateTime savedTarget) {
        this.originalAmount = amount;
        this.originalUnit = unit;
        this.totalSeconds = savedTotalSeconds;
        this.targetTime.set(savedTarget);
        this.pausedDuration = null;
        this.finished.set(false);
        this.running.set(true);
        updateRemaining(); // will mark finished if target is in the past
    }

    /** Restore a paused timer from saved remaining seconds */
    public void restorePaused(long amount, TimeUnit unit, long savedTotalSeconds, long savedRemainingSeconds) {
        this.originalAmount = amount;
        this.originalUnit = unit;
        this.totalSeconds = savedTotalSeconds;
        this.pausedDuration = Duration.ofSeconds(savedRemainingSeconds);
        this.remainingSeconds.set(savedRemainingSeconds);
        this.running.set(false);
        this.finished.set(false);
    }

    /** Restore a finished timer */
    public void restoreFinished(long amount, TimeUnit unit, long savedTotalSeconds) {
        this.originalAmount = amount;
        this.originalUnit = unit;
        this.totalSeconds = savedTotalSeconds;
        this.remainingSeconds.set(0);
        this.running.set(false);
        this.finished.set(true);
    }

    public long getOriginalAmount() { return originalAmount; }
    public TimeUnit getOriginalUnit() { return originalUnit; }
    public long getTotalSeconds() { return totalSeconds; }

    public double getProgress() {
        if (totalSeconds <= 0) return 0;
        long remaining = getRemainingSeconds();
        return 1.0 - ((double) remaining / totalSeconds);
    }

    // Property accessors
    public StringProperty themeNameProperty() { return themeName; }
    public String getThemeName() { return themeName.get(); }
    public void setThemeName(String name) { this.themeName.set(name); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String desc) { this.description.set(desc); }

    public StringProperty classNameProperty() { return className; }
    public String getClassName() { return className.get(); }
    public void setClassName(String cn) { this.className.set(cn != null ? cn : ""); }

    public ObjectProperty<LocalDateTime> targetTimeProperty() { return targetTime; }

    public BooleanProperty runningProperty() { return running; }
    public boolean isRunning() { return running.get(); }

    public BooleanProperty finishedProperty() { return finished; }
    public boolean isFinished() { return finished.get(); }

    public LongProperty remainingSecondsProperty() { return remainingSeconds; }
    public long getRemainingSeconds() { return remainingSeconds.get(); }
}
