module no.countdown {
    requires javafx.controls;

    exports no.countdown;
    exports no.countdown.model;
    exports no.countdown.ui;

    opens no.countdown to javafx.controls;
    opens no.countdown.model to javafx.controls;
    opens no.countdown.ui to javafx.controls;
}
