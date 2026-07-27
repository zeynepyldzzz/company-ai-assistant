package com.company.assistant.shuttle;

public class ShuttleRoutePlateResponse {

    private Integer id;
    private String name;
    private String plateNumber;

    public ShuttleRoutePlateResponse(Integer id, String name, String plateNumber) {
        this.id = id;
        this.name = name;
        this.plateNumber = plateNumber;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getPlateNumber() { return plateNumber; }
}
