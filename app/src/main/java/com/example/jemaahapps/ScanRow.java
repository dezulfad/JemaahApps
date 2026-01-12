package com.example.jemaahapps;

public class ScanRow {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private int type;
    private String programName; // header
    private String name;        // item
    private String phone;       // item

    public static ScanRow header(String programName) {
        ScanRow r = new ScanRow();
        r.type = TYPE_HEADER;
        r.programName = programName;
        return r;
    }

    public static ScanRow item(String name, String phone) {
        ScanRow r = new ScanRow();
        r.type = TYPE_ITEM;
        r.name = name;
        r.phone = phone;
        return r;
    }

    public int getType() { return type; }
    public String getProgramName() { return programName; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
}
