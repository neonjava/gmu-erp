package in.neonjava.erp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty")
public class Faculty extends Person {

    @Column(nullable = false, unique = true, length = 20)
    private String empId;

    private String designation;

    @Override
    public String getCode() { return empId; }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId == null ? null : empId.trim().toUpperCase(); }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
