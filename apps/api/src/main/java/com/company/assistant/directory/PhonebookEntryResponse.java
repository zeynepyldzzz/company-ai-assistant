package com.company.assistant.directory;

public class PhonebookEntryResponse {

    private Integer id;
    private String name;
    private String extension;
    private String departmentName;

    public PhonebookEntryResponse(Employee employee) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.extension = employee.getPhone();
        if (employee.getDepartment() != null) {
            this.departmentName = employee.getDepartment().getName();
        }
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getExtension() { return extension; }
    public String getDepartmentName() { return departmentName; }
}
