package in.neonjava.erp.api;

import in.neonjava.erp.model.*;
import in.neonjava.erp.service.AttendanceService;
import in.neonjava.erp.service.TimetableService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Plain JSON shapes for the React frontend, so entities (and their lazy links) never leave the server. */
public final class Dto {
    private Dto() {}

    public record StudentInfo(Long id, String usn, String name, String department, String departmentName,
                              int semester, String section, String email, String phone) {
        static StudentInfo of(Student s) {
            return s == null ? null : new StudentInfo(s.getId(), s.getUsn(), s.getName(),
                    s.getDepartment() == null ? null : s.getDepartment().getCode(),
                    s.getDepartment() == null ? null : s.getDepartment().getName(),
                    s.getSemester(), s.getSection(), s.getEmail(), s.getPhone());
        }
    }

    public record FacultyInfo(Long id, String empId, String name, String designation, String department,
                              String departmentName, String email, String phone) {
        static FacultyInfo of(Faculty f) {
            return f == null ? null : new FacultyInfo(f.getId(), f.getEmpId(), f.getName(), f.getDesignation(),
                    f.getDepartment() == null ? null : f.getDepartment().getCode(),
                    f.getDepartment() == null ? null : f.getDepartment().getName(), f.getEmail(), f.getPhone());
        }
    }

    public record Me(String username, String displayName, String role, boolean mustChangePassword,
                     LocalDateTime lastLogin, StudentInfo student, FacultyInfo faculty) {
        static Me of(AppUser u) {
            return new Me(u.getUsername(), u.getDisplayName(), u.getRole().name(), u.isMustChangePassword(),
                    u.getLastLogin(), StudentInfo.of(u.getStudent()), FacultyInfo.of(u.getFaculty()));
        }
    }

    public record CourseRow(Long id, String code, String name, int credits, int semester, String faculty) {
        static CourseRow of(Course c) {
            return new CourseRow(c.getId(), c.getCode(), c.getName(), c.getCredits(), c.getSemester(),
                    c.getFaculty() == null ? null : c.getFaculty().getName());
        }
    }

    public record NoticeRow(Long id, String title, String body, LocalDate postedOn) {
        static NoticeRow of(Notice n) { return new NoticeRow(n.getId(), n.getTitle(), n.getBody(), n.getPostedOn()); }
    }

    public record EventRow(Long id, String title, String category, String venue, LocalDate date) {
        static EventRow of(Event e) {
            return new EventRow(e.getId(), e.getTitle(), e.getCategory().getLabel(), e.getVenue(), e.getDate());
        }
    }

    public record Stat(String label, String value, String link, Double ring) {}

    public record Dashboard(String greeting, List<Stat> stats, List<TimetableService.TodayClass> today,
                            List<NoticeRow> notices, List<EventRow> events, List<CourseRow> myCourses) {}

    public record AttendanceRow(String code, String name, long attended, long total, double percent,
                                boolean shortage, long classesNeeded) {
        static AttendanceRow of(AttendanceService.Summary s) {
            return new AttendanceRow(s.course().getCode(), s.course().getName(), s.attended(), s.total(),
                    s.percent(), s.shortage(), s.classesNeeded());
        }
    }

    public record ResultRow(String code, String name, int credits, Integer cie1, Integer cie2, int cie,
                            Integer see, Integer total, String grade) {
        static ResultRow of(Mark m) {
            return new ResultRow(m.getCourse().getCode(), m.getCourse().getName(), m.getCourse().getCredits(),
                    m.getCie1(), m.getCie2(), m.getCieTotal(), m.getSee(),
                    m.getSee() == null ? null : m.getTotal(), m.getSee() == null ? null : m.getGrade());
        }
    }

    public record Slot(String day, int period, String code, String name, String room) {}

    /** When a subject meets during the week. */
    public record Meeting(String day, int period, String time, String room) {}

    /** Everything the subject pop-up shows: schedule, teacher, and the student's own attendance and marks. */
    public record Subject(Long id, String code, String name, int credits, String faculty, String facultyDesignation,
                          boolean registered, List<Meeting> meetings,
                          Long attended, Long total, Double percent, Boolean shortage, Long classesNeeded,
                          Integer cie1, Integer cie2, Integer cie, Integer see, Integer marksTotal, String grade) {}

    public record Timetable(String title, List<String> days, List<String> times, List<Slot> slots) {}

    public record OfferedCourse(CourseRow course, boolean registered) {}

    public record StudentAccount(StudentInfo student, String status, LocalDateTime lastLogin,
                                 LocalDateTime lockedUntil, boolean enabled, boolean locked) {}
}
