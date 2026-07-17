package com.company.assistant.directory;

public class EmployeeResponse {

    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String officeStatus;
    private Integer departmentId;
    private String departmentName;

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
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getOfficeStatus() { return officeStatus; }
    public Integer getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
}
