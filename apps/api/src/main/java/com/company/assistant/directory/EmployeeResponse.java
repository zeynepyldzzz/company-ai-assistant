package com.company.assistant.directory;

public class EmployeeResponse {

    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String officeStatus;
    private Integer departmentId;
    private String departmentName;
    private Integer roleId;
    private String roleName;

    public EmployeeResponse(Employee employee) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.email = employee.getEmail();
        this.phone = employee.getPhone();
        this.officeStatus = employee.getOfficeStatus();
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
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getOfficeStatus() { return officeStatus; }
    public Integer getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public Integer getRoleId() { return roleId; }
    public String getRoleName() { return roleName; }
}
