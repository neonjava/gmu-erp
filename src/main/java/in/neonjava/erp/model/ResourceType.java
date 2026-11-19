package in.neonjava.erp.model;

public enum ResourceType {
    LAB("Lab", true), CLASSROOM("Classroom", false), HALL("Seminar hall", false), EQUIPMENT("Equipment", true);

    private final String label;
    private final boolean studentBookable;

    ResourceType(String label, boolean studentBookable) {
        this.label = label;
        this.studentBookable = studentBookable;
    }

    public String getLabel() { return label; }

    /** Students may book labs and equipment; rooms and halls need a faculty member or admin. */
    public boolean isStudentBookable() { return studentBookable; }
}
