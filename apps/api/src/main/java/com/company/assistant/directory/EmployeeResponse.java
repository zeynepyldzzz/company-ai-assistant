package com.company.assistant.directory;

import com.company.assistant.schedule.ScheduleStatus;

public class EmployeeResponse {

    private Integer id;
    private String name;
    // A-35 (#196): tam ad (name) DA donmeye devam ediyor — istemcilerin cogu tek parca
    // gosteriyor ve ikisini de vermek her cagiranin ayri birlestirme yapmasini onluyor.
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String officeStatus;
    private Integer departmentId;
    private String departmentName;
    private Integer roleId;
    private String roleName;

    /**
     * A-32 (#188): ofis durumu BILINMEDEN kurulan yanit — {@code officeStatus} null kalir.
     *
     * <p>Kayit/guncelleme uclari bu kurucuyu kullanir: orada donen sey "kaydettigin calisan",
     * "bugun nerede oldugu" degil. Durumun dolu gelmesi gereken yerler (rehber listesi ve
     * tek calisan ucu) asagidaki iki argumanli kurucuyu kullanir.
     *
     * <p>Kolondan ({@code employee.office_status}) OKUNMUYOR. O kolon duragan ve elle set
     * ediliyordu; plandan bagimsiz yasadigi icin celiskiye sebep oluyordu.
     */
    public EmployeeResponse(Employee employee) {
        this(employee, null);
    }

    /**
     * A-32 (#188): ofis durumu BUGUNUN planindan gelir.
     *
     * @param todayStatus bugunun {@code schedule_day} kaydi; plan yoksa {@code null} —
     *                    bu durumda {@code officeStatus} null doner ve istemci
     *                    "Plan girilmedi" gosterir
     */
    public EmployeeResponse(Employee employee, ScheduleStatus todayStatus) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.firstName = employee.getFirstName();
        this.lastName = employee.getLastName();
        this.email = employee.getEmail();
        this.phone = employee.getPhone();
        this.officeStatus = OfficeStatusLabels.labelFor(todayStatus);
        if (employee.getDepartment() != null) {
            this.departmentId = employee.getDepartment().getId();
            this.departmentName = employee.getDepartment().getName();
        }
        // C-11 (#85): rol yonetim ekraninda mevcut rolun gorunmesi icin.
        if (employee.getRole() != null) {
            this.roleId = employee.getRole().getId();
            this.roleName = employee.getRole().getName();
        }
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getOfficeStatus() { return officeStatus; }
    public Integer getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public Integer getRoleId() { return roleId; }
    public String getRoleName() { return roleName; }
}
