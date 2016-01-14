package io.intrepid.russell.checkin;

import android.app.Application;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import okhttp3.logging.HttpLoggingInterceptor.Logger;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import timber.log.Timber;

public class CheckInApplication extends Application {
    private static SlackApi slackApi;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

    }

    public static SlackApi getApi() {
        if (slackApi == null) {
            Logger logger = new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Timber.v(message);
                }
            };
            Level level = BuildConfig.DEBUG ? Level.BODY : HttpLoggingInterceptor.Level.BASIC;
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new HttpLoggingInterceptor(logger).setLevel(level))
                    .build();
            slackApi = new Retrofit.Builder()
                    .baseUrl(SlackApi.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
                    .create(SlackApi.class);
        }
        return slackApi;
    }
}
