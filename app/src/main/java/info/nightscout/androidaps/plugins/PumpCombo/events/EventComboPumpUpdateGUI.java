package info.nightscout.androidaps.plugins.PumpCombo.events;

/**
 * Created by mike on 24.05.2017.
 */

public class EventComboPumpUpdateGUI {
    public EventComboPumpUpdateGUI() {}

    public EventComboPumpUpdateGUI(String status) {
        this.status = status;
    }

    public String status;
}
