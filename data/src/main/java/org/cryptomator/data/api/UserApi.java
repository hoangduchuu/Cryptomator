package org.cryptomator.data.api;

import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.models.userprofile.RefreshTokenResponse;

import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;
import retrofit2.http.HEAD;

public interface UserApi {
    @GET("/account")
    Call<UserProfile> getUserProfile(
        @Header("Authorization") String authorization,
        @Header("Content-Type") String contentType
    );

    @GET("/account/avatar")
    Call<ResponseBody> getUserAvatar(
        @Header("Authorization") String authorization,
        @Query("userId") String userId
    );


    @POST("/account?login=true")
    Call<UserProfile> login(
        @Header("Authorization") String authorization,
        @Header("Content-Type") String contentType,
        @Body Object deviceArgs
    );

    @POST("https://cognito-idp.us-east-1.amazonaws.com/")
    Call<RefreshTokenResponse> refreshToken(
        @Header("Content-Type") String contentType,
        @Header("X-Amz-Target") String target,
        @Body Object requestBody
    );

    @HEAD("/account/poll")
    Call<Void> pollUserStatus(
        @Query("userId") String userId,
        @Query("eTag") String eTag
    );
} 