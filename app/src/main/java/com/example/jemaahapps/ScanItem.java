package com.example.jemaahapps;

public class ScanItem {
    private String name;
    private String phone;
    private String programName;

    public ScanItem(String name, String phone, String programName) {
        this.name = name;
        this.phone = phone;
        this.programName = programName;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getProgramName() { return programName; }
}