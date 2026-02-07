# ChronoX

A JavaFX countdown timer with a dark blue/purple theme. Create multiple timers that run independently, organize them by class, and pick up where you left off.

## Features

- **Multiple independent timers** — run as many as you need simultaneously
- **Classes (categories)** — assign an optional class to each timer. Timers get a colored tag, and clicking the tag filters the list to that class
- **Progress bars** — each timer card shows a visual progress indicator
- **Persistence** — timers are saved to `~/.chronox/timers.txt` and restored on startup, including running/paused state
- **Sound alert** — system beep when a timer finishes
- **Finished timers float to top** — completed timers move to the top of the list for visibility
- **Edit timers** — change name, description, or class on an existing timer
- **Drag to reorder** — drag the ☰ handle to rearrange timers
- **Pause / Resume / Reset** — full control over each timer
- **Flexible time units** — minutes, hours, days, weeks, or months

## Requirements

- Java 17+
- Maven 3.6+

## Run

```bash
mvn javafx:run
```

## Usage

1. Click **"+ New Timer"** in the left panel
2. Enter a name, optional description and class, amount, and time unit
3. Click **"Done"** — the countdown starts immediately
4. Click a timer card to see the large countdown display
5. Use **Pause**, **Reset**, **Edit**, or **Delete** to control the selected timer
6. Click a **class tag** to filter the list — click **✕** to clear the filter
7. Drag the **☰** handle to reorder timers
8. When a timer finishes, press **"Done"** on the card to restart
9. Close the app — all timers are saved and restored on next launch

## Project Structure

```
src/main/java/
├── module-info.java
└── no/countdown/
    ├── CountdownApp.java          # Application entry point
    ├── model/
    │   └── CountdownTimer.java    # Timer model with JavaFX properties
    └── ui/
        └── TimerController.java   # Main UI controller (BorderPane)
src/main/resources/
└── styles.css                     # Dark blue/purple theme
```

## Data

Timer state is persisted to `~/.chronox/timers.txt`. Running timers resume from their saved target time, paused timers keep their remaining duration, and finished timers stay marked as done.

## Tech Stack

- Java 17
- JavaFX 21
- Maven with javafx-maven-plugin

---                                                                           
  Built with the assistance of https://claude.ai by Anthropic.     
