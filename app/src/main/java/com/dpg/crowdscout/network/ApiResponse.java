package com.dpg.crowdscout.network;

public class ApiResponse<T> {
    public T data;

    public String code;

    public String status;

    public boolean wasSuccessful() {
        return code != null && code.equals("200");
    }
}
