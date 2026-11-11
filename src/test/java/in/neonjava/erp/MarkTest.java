package in.neonjava.erp;

import in.neonjava.erp.model.Mark;
import in.neonjava.erp.service.AttendanceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkTest {

    @Test
    void gradeNeedsSee() {
        Mark m = new Mark();
        m.setCie1(20);
        m.setCie2(22);
        assertEquals("-", m.getGrade());
        m.setSee(45);
        assertEquals(87, m.getTotal());
        assertEquals("A+", m.getGrade());
        assertEquals(9, m.getGradePoint());
    }

    @Test
    void failBelow40() {
        Mark m = new Mark();
        m.setCie1(10);
        m.setCie2(10);
        m.setSee(15);
        assertEquals("F", m.getGrade());
    }

    @Test
    void classesNeededToReach75() {
        // 6 of 10 attended -> need x more where (6+x)/(10+x) >= 0.75 -> x = 6
        var s = new AttendanceService.Summary(null, 6, 10);
        assertTrue(s.shortage());
        assertEquals(6, s.classesNeeded());
    }
}
