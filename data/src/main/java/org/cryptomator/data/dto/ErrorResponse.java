package org.cryptomator.data.dto;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("reason")
    private String reason;
    
    @SerializedName("success")
    private boolean success;

    public String getReason() {
        return reason;
    }

    public boolean isSuccess() {
        return success;
    }
} 