package in.neonjava.erp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Login account. Linked to a Student or Faculty record depending on role. */
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @OneToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @OneToOne
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    private String displayName;

    /** Turned off by the admin: the person cannot sign in. */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = true;

    /** Set for new accounts and after a reset: the person must pick their own password first. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean mustChangePassword;

    /** Wrong passwords in a row; too many locks the account for a while. */
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int failedLogins;

    private LocalDateTime lockedUntil;
    private LocalDateTime lastLogin;

    public AppUser() {}

    public AppUser(String username, String password, Role role, String displayName) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.displayName = displayName;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public int getFailedLogins() { return failedLogins; }
    public void setFailedLogins(int failedLogins) { this.failedLogins = failedLogins; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public boolean isLocked() { return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now()); }

    /** One word for the admin list: Active, New (must set a password), Locked or Off. */
    public String getStatus() {
        if (!enabled) return "Off";
        if (isLocked()) return "Locked";
        if (mustChangePassword) return "New";
        return "Active";
    }
}
