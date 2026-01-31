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
    private final ObjectProperty<LocalDateTime> targetTime = new SimpleObjectProperty<>();
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty finished = new SimpleBooleanProperty(false);
    private final LongProperty remainingSeconds = new SimpleLongProperty(0);

    private Duration pausedDuration;
    private long originalAmount;
    private TimeUnit originalUnit;

    public CountdownTimer(String themeName, String description) {
        this.themeName.set(themeName);
        this.description.set(description);
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

    public long getOriginalAmount() { return originalAmount; }
    public TimeUnit getOriginalUnit() { return originalUnit; }

    // Property accessors
    public StringProperty themeNameProperty() { return themeName; }
    public String getThemeName() { return themeName.get(); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }

    public ObjectProperty<LocalDateTime> targetTimeProperty() { return targetTime; }

    public BooleanProperty runningProperty() { return running; }
    public boolean isRunning() { return running.get(); }

    public BooleanProperty finishedProperty() { return finished; }
    public boolean isFinished() { return finished.get(); }

    public LongProperty remainingSecondsProperty() { return remainingSeconds; }
    public long getRemainingSeconds() { return remainingSeconds.get(); }
}
