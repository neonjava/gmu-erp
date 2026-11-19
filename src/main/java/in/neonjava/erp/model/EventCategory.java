package in.neonjava.erp.model;

public enum EventCategory {
    TECHNICAL("Technical"), WORKSHOP("Workshop"), SEMINAR("Seminar"), CULTURAL("Cultural"), SPORTS("Sports");

    private final String label;

    EventCategory(String label) { this.label = label; }

    public String getLabel() { return label; }
}
