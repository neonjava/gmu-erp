package in.neonjava.erp.service;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, updates and deletes students and faculty together with their login accounts.
 * New accounts use the USN / employee ID as username and a default password.
 */
@Service
public class AccountService {

    public static final String DEFAULT_STUDENT_PASSWORD = "student123";
    public static final String DEFAULT_FACULTY_PASSWORD = "faculty123";

    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final CourseRepository courses;
    private final UserRepository users;
    private final RegistrationRepository registrations;
    private final AttendanceRepository attendance;
    private final MarkRepository marks;
    private final EventRegistrationRepository eventRegistrations;
    private final BookingRepository bookings;
    private final PasswordEncoder encoder;

    public AccountService(StudentRepository students, FacultyRepository faculty, CourseRepository courses,
                          UserRepository users, RegistrationRepository registrations,
                          AttendanceRepository attendance, MarkRepository marks,
                          EventRegistrationRepository eventRegistrations, BookingRepository bookings,
                          PasswordEncoder encoder) {
        this.students = students;
        this.faculty = faculty;
        this.courses = courses;
        this.users = users;
        this.registrations = registrations;
        this.attendance = attendance;
        this.marks = marks;
        this.eventRegistrations = eventRegistrations;
        this.bookings = bookings;
        this.encoder = encoder;
    }

    // ---------- Students ----------

    @Transactional
    public Student saveStudent(Student s) {
        if (s.getUsn() == null || s.getUsn().isBlank()) throw new IllegalArgumentException("USN is required");
        if (s.getName() == null || s.getName().isBlank()) throw new IllegalArgumentException("Name is required");

        if (s.getId() == null) {
            if (students.existsByUsn(s.getUsn()) || users.existsByUsernameIgnoreCase(s.getUsn())) {
                throw new IllegalArgumentException("USN " + s.getUsn() + " already exists");
            }
            Student saved = students.save(s);
            AppUser u = new AppUser(saved.getUsn(), encoder.encode(DEFAULT_STUDENT_PASSWORD), Role.STUDENT, saved.getName());
            u.setStudent(saved);
            users.save(u);
            return saved;
        }

        // Update: USN is the login name, so it is not editable.
        Student existing = students.findById(s.getId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        existing.setName(s.getName());
        existing.setEmail(s.getEmail());
        existing.setPhone(s.getPhone());
        existing.setDepartment(s.getDepartment());
        existing.setSemester(s.getSemester());
        existing.setSection(s.getSection());
        return students.save(existing);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student s = students.findById(id).orElseThrow(() -> new IllegalArgumentException("Student not found"));
        users.findByStudent(s).ifPresent(bookings::deleteByBookedBy);
        users.deleteByStudent(s);
        eventRegistrations.deleteByStudent(s);
        attendance.deleteByStudent(s);
        marks.deleteByStudent(s);
        registrations.deleteByStudent(s);
        students.delete(s);
    }

    // ---------- Faculty ----------

    @Transactional
    public Faculty saveFaculty(Faculty f) {
        if (f.getEmpId() == null || f.getEmpId().isBlank()) throw new IllegalArgumentException("Employee ID is required");
        if (f.getName() == null || f.getName().isBlank()) throw new IllegalArgumentException("Name is required");

        if (f.getId() == null) {
            if (faculty.existsByEmpId(f.getEmpId()) || users.existsByUsernameIgnoreCase(f.getEmpId())) {
                throw new IllegalArgumentException("Employee ID " + f.getEmpId() + " already exists");
            }
            Faculty saved = faculty.save(f);
            AppUser u = new AppUser(saved.getEmpId(), encoder.encode(DEFAULT_FACULTY_PASSWORD), Role.FACULTY, saved.getName());
            u.setFaculty(saved);
            users.save(u);
            return saved;
        }

        Faculty existing = faculty.findById(f.getId())
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
        existing.setName(f.getName());
        existing.setEmail(f.getEmail());
        existing.setPhone(f.getPhone());
        existing.setDepartment(f.getDepartment());
        existing.setDesignation(f.getDesignation());
        return faculty.save(existing);
    }

    @Transactional
    public void deleteFaculty(Long id) {
        Faculty f = faculty.findById(id).orElseThrow(() -> new IllegalArgumentException("Faculty not found"));
        for (Course c : courses.findByFacultyOrderByCode(f)) {
            c.setFaculty(null);
            courses.save(c);
        }
        users.findByFaculty(f).ifPresent(bookings::deleteByBookedBy);
        users.deleteByFaculty(f);
        faculty.delete(f);
    }

    // ---------- Passwords ----------

    @Transactional
    public void changePassword(AppUser user, String current, String next, String confirm) {
        if (!encoder.matches(current, user.getPassword())) throw new IllegalArgumentException("Current password is wrong");
        if (next == null || next.length() < 6) throw new IllegalArgumentException("New password must be at least 6 characters");
        if (!next.equals(confirm)) throw new IllegalArgumentException("New passwords do not match");
        user.setPassword(encoder.encode(next));
        users.save(user);
    }

    @Transactional
    public void resetPassword(AppUser user) {
        String pwd = user.getRole() == Role.FACULTY ? DEFAULT_FACULTY_PASSWORD : DEFAULT_STUDENT_PASSWORD;
        user.setPassword(encoder.encode(pwd));
        users.save(user);
    }
}
