package com.company.assistant.shuttle;

public class ShuttleRoutePlateResponse {

    private Integer id;
    private String plateNumber;

    public ShuttleRoutePlateResponse(Integer id, String plateNumber) {
        this.id = id;
        this.plateNumber = plateNumber;
    }

    public Integer getId() { return id; }
    public String getPlateNumber() { return plateNumber; }
}
