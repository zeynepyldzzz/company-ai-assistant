package com.company.assistant.directory;

public class DepartmentResponse {

    private Integer id;
    private String name;
    private String responsibilities;
    private Integer managerId;
    private String managerName;
    private String managerEmail;
    private String managerPhone;

    public DepartmentResponse(Department department) {
        this.id = department.getId();
        this.name = department.getName();
        this.responsibilities = department.getResponsibilities();
        if (department.getManager() != null) {
            this.managerId = department.getManager().getId();
            this.managerName = department.getManager().getName();
            this.managerEmail = department.getManager().getEmail();
            this.managerPhone = department.getManager().getPhone();
        }
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getResponsibilities() { return responsibilities; }
    public Integer getManagerId() { return managerId; }
    public String getManagerName() { return managerName; }
    public String getManagerEmail() { return managerEmail; }
    public String getManagerPhone() { return managerPhone; }
}
