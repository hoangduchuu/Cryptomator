package org.cryptomator.data.api;

import android.content.Context;

import org.cryptomator.data.BuildConfig;
import org.cryptomator.data.cloud.okhttplogging.HttpLoggingInterceptor;
import org.cryptomator.data.util.NetworkTimeout;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String BASE_URL = BuildConfig.API_BASE_URL;
    
    private static String bodyToString(final RequestBody request) {
        try {
            final RequestBody copy = request;
            final Buffer buffer = new Buffer();
            if (copy != null) {
                copy.writeTo(buffer);
                return buffer.readUtf8();
            }
            return "";
        } catch (final IOException e) {
            return "Error reading request body: " + e.getMessage();
        }
    }
    
    public static Retrofit getClient(Context context) {
        HttpLoggingInterceptor.Logger logger = message -> {
            if (message.startsWith("-->") || message.startsWith("<--")) {
                Timber.tag("OkHttp").d("╔══════════════════════════════════════════════════════════════════════════════");
                Timber.tag("OkHttp").d("║ %s", message);
            } else if (message.startsWith("|")) {
                Timber.tag("OkHttp").d("║ %s", message);
            } else {
                Timber.tag("OkHttp").d("║ %s", message);
            }
            if (message.startsWith("<-- END HTTP")) {
                Timber.tag("OkHttp").d("╚══════════════════════════════════════════════════════════════════════════════");
            }
        };

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(logger, context);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    long startNs = System.nanoTime();
                    
                    // Log request details
                    Timber.tag("OkHttp").d("╔══════════════════════════════════════════════════════════════════════════════");
                    Timber.tag("OkHttp").d("║ Request: %s %s", request.method(), request.url());
                    Timber.tag("OkHttp").d("║ Request Headers:");
                    request.headers().forEach(header -> 
                        Timber.tag("OkHttp").d("║   %s: %s", header.getFirst(), header.getSecond())
                    );
                    
                    if (request.body() != null) {
                        String bodyString = bodyToString(request.body());
                        Timber.tag("OkHttp").d("║ Request Body: %s", bodyString);
                    }
                    
                    // Execute request
                    Response response = chain.proceed(request);
                    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
                    
                    // Log response details
                    Timber.tag("OkHttp").d("║");
                    Timber.tag("OkHttp").d("║ Response: %s %s", response.code(), response.message());
                    Timber.tag("OkHttp").d("║ Response Headers:");
                    response.headers().forEach(header -> 
                        Timber.tag("OkHttp").d("║   %s: %s", header.getFirst(), header.getSecond())
                    );
                    
                    ResponseBody responseBody = response.body();
                    String responseBodyString = responseBody != null ? responseBody.string() : null;
                    
                    Timber.tag("OkHttp").d("║");
                    Timber.tag("OkHttp").d("║ Response Time: %dms", tookMs);
                    Timber.tag("OkHttp").d("║ Response Code: %d", response.code());
                    Timber.tag("OkHttp").d("║ Response Protocol: %s", response.protocol());
                    Timber.tag("OkHttp").d("║ Response Cache: %s", response.cacheResponse() != null ? "HIT" : "MISS");
                    
                    if (responseBodyString != null) {
                        Timber.tag("OkHttp").d("║ Response Body: %s", responseBodyString);
                        ResponseBody newBody = ResponseBody.create(responseBody.contentType(), responseBodyString);
                        return response.newBuilder().body(newBody).build();
                    }
                    
                    Timber.tag("OkHttp").d("╚══════════════════════════════════════════════════════════════════════════════");
                    return response;
                })
                .addInterceptor(loggingInterceptor)
                .connectTimeout(NetworkTimeout.CONNECTION.getTimeout(), NetworkTimeout.CONNECTION.getUnit())
                .readTimeout(NetworkTimeout.READ.getTimeout(), NetworkTimeout.READ.getUnit())
                .writeTimeout(NetworkTimeout.WRITE.getTimeout(), NetworkTimeout.WRITE.getUnit())
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
} 