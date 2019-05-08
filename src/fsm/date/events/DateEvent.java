package fsm.date.events;

public abstract class DateEvent {

    public String name;

    public DateEvent(String name) {
        this.name = name;
    }

    public String toString() {
        return name.substring(1, name.length() - 1);
    }

    public abstract String toDefinition();
}
