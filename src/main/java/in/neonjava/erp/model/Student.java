package in.neonjava.erp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student extends Person {

    @Column(nullable = false, unique = true, length = 20)
    private String usn;

    private int semester = 1;

    @Column(length = 5)
    private String section = "A";

    @Override
    public String getCode() { return usn; }

    public String getUsn() { return usn; }
    public void setUsn(String usn) { this.usn = usn == null ? null : usn.trim().toUpperCase(); }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}
