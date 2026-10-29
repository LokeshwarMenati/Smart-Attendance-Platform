package com.smartattendance.platform.common.config;

import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;
import com.smartattendance.platform.employee.entity.EmployeeShift;
import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.common.util.BusinessTimeService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Seeds initial demo accounts only in non-test profiles when the employee table is empty.
 */
@Component
@Profile("local")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceRepository attendanceRepository;
    private final BusinessTimeService businessTimeService;

    public DataInitializer(EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder,
                           AttendanceRepository attendanceRepository,
                           BusinessTimeService businessTimeService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.attendanceRepository = attendanceRepository;
        this.businessTimeService = businessTimeService;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.count() == 0) {
            log.info("Database is empty. Seeding initial development accounts...");

            // 1. Admin
            Employee admin = new Employee(
                    UUID.randomUUID(),
                    "ADM-001",
                    "System",
                    "Admin",
                    "admin@smartattendance.com",
                    passwordEncoder.encode("Admin@12345"),
                    EmployeeRole.ADMIN,
                    EmployeeStatus.ACTIVE
            );
            employeeRepository.save(admin);

            // 2. Manager
            Employee manager = new Employee(
                    UUID.randomUUID(),
                    "MGR-001",
                    "Shift",
                    "Manager",
                    "manager@smartattendance.com",
                    passwordEncoder.encode("Manager@12345"),
                    EmployeeRole.MANAGER,
                    EmployeeStatus.ACTIVE
            );
            employeeRepository.save(manager);

            // 3. Employee
            Employee employee = new Employee(
                    UUID.randomUUID(),
                    "EMP-001",
                    "Alex",
                    "Rivera",
                    "employee@smartattendance.com",
                    passwordEncoder.encode("Employee@12345"),
                    EmployeeRole.EMPLOYEE,
                    EmployeeStatus.ACTIVE
            );
            employeeRepository.save(employee);

            String[][] demoEmployees = {
                    {"EMP-101", "Priya", "Sharma", "priya.sharma@example.com"},
                    {"EMP-102", "Rahul", "Nair", "rahul.nair@example.com"},
                    {"EMP-103", "Ananya", "Rao", "ananya.rao@example.com"},
                    {"EMP-104", "Vikram", "Singh", "vikram.singh@example.com"},
                    {"EMP-105", "Meera", "Patel", "meera.patel@example.com"},
                    {"EMP-106", "Arjun", "Kumar", "arjun.kumar@example.com"},
                    {"EMP-107", "Neha", "Das", "neha.das@example.com"},
                    {"EMP-108", "Karan", "Mehta", "karan.mehta@example.com"},
                    {"EMP-109", "Isha", "Verma", "isha.verma@example.com"},
                    {"EMP-110", "Aditya", "Joshi", "aditya.joshi@example.com"},
                    {"EMP-111", "Sana", "Khan", "sana.khan@example.com"},
                    {"EMP-112", "Rohan", "Iyer", "rohan.iyer@example.com"},
                    {"EMP-113", "Tara", "Menon", "tara.menon@example.com"},
                    {"EMP-114", "Dev", "Shah", "dev.shah@example.com"},
                    {"EMP-115", "Kavya", "Reddy", "kavya.reddy@example.com"},
                    {"EMP-116", "Nikhil", "Bose", "nikhil.bose@example.com"},
                    {"EMP-117", "Asha", "Gupta", "asha.gupta@example.com"},
                    {"EMP-118", "Sameer", "Ali", "sameer.ali@example.com"}
            };
            for (int i = 0; i < demoEmployees.length; i++) {
                String[] data = demoEmployees[i];
                Employee demo = new Employee(UUID.randomUUID(), data[0], data[1], data[2], data[3],
                        passwordEncoder.encode("Employee@12345"), EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE);
                demo.setShift(EmployeeShift.values()[i % EmployeeShift.values().length]);
                employeeRepository.save(demo);
            }

            log.info("Development demo accounts seeded successfully: admin@smartattendance.com, manager@smartattendance.com, employee@smartattendance.com");
        }
        seedDemoAttendance();
    }

    private void seedDemoAttendance() {
        LocalDate today = businessTimeService.getCurrentBusinessDate();
        ZoneId zone = businessTimeService.getBusinessZoneId();
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(employee -> employee.getRole() == EmployeeRole.EMPLOYEE)
                .filter(employee -> employee.getEmployeeCode().startsWith("EMP-1"))
                .toList();

        for (int dayOffset = 2; dayOffset >= 0; dayOffset--) {
            LocalDate date = today.minusDays(dayOffset);
            int presentCount = dayOffset == 0 ? Math.min(12, employees.size()) : Math.min(16 + (2 - dayOffset), employees.size());
            for (int index = 0; index < presentCount; index++) {
                Employee employee = employees.get(index);
                if (attendanceRepository.findByEmployeeAndAttendanceDate(employee, date).isPresent()) {
                    continue;
                }
                LocalTime shiftStart = switch (employee.getShift()) {
                    case MORNING -> LocalTime.of(6, 0);
                    case EVENING -> LocalTime.of(14, 0);
                    case NIGHT -> LocalTime.of(22, 0);
                };
                int lateMinutes = (index % 5 == 0) ? 25 : (index % 3 == 0 ? 8 : 0);
                Instant clockIn = date.atTime(shiftStart.plusMinutes(lateMinutes)).atZone(zone).toInstant();
                AttendanceRecord record = new AttendanceRecord(UUID.randomUUID(), employee, date, clockIn,
                        "Demo attendance - shift progress recorded");
                boolean openToday = dayOffset == 0 && index < 4;
                if (!openToday) {
                    Instant clockOut = clockIn.plus(Duration.ofMinutes(465 + (index % 4) * 20L));
                    record.setClockOutTime(clockOut);
                    record.setDurationMinutes((int) Duration.between(clockIn, clockOut).toMinutes());
                    record.setStatus(AttendanceStatus.COMPLETED);
                }
                attendanceRepository.save(record);
            }
        }
        log.info("Seeded demo attendance records for analytics dashboard.");
    }
}
