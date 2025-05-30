package org.cryptomator.data.dto;

import com.google.gson.annotations.SerializedName;

public class BaseResponseDTO {
    @SerializedName("success")
    private boolean success;

    @SerializedName("reason")
    private String reason;

    public boolean isSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }
} 