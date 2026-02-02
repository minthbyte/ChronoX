# ChronoX

A JavaFX countdown timer with a dark blue/purple theme. Create multiple timers that run independently.

## Features

- **Multiple independent timers** - run as many as you need simultaneously
- **Instant start** - countdown begins the moment you click "Done"
- **User-controlled restart** - press "Done" on a finished timer to restart it
- **Drag to reorder** - drag the ☰ handle to rearrange timers
- **Pause / Resume / Reset** - full control over each timer
- **Delete** - remove timers you no longer need
- **Flexible time units** - minutes, hours, days, weeks, or months
- **Built-in guide** - usage instructions shown in the app

## Requirements

- Java 17+
- Maven 3.6+

## Run

```bash
mvn javafx:run
```

## Usage

1. Click **"+ New Timer"** in the left panel
2. Enter a name, amount, and time unit
3. Click **"Done"** — the countdown starts immediately
4. Click a timer card to see the large countdown display
5. Use **Pause**, **Reset**, or **Delete** to control the selected timer
6. Drag the **☰** handle to reorder timers
7. When a timer finishes, press **"Done"** on the card to restart

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

## Tech Stack

- Java 17
- JavaFX 21
- Maven with javafx-maven-plugin

---                                                                           
  Built with the assistance of https://claude.ai by Anthropic.     
