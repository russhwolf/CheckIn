package io.intrepid.russell.checkin;

import android.app.Application;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
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
            HttpLoggingInterceptor.Logger logger = new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Timber.v(message);
                }
            };
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new HttpLoggingInterceptor(logger).setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build();
            slackApi = new Retrofit.Builder()
                    .baseUrl("https://hooks.slack.com/services/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
                    .create(SlackApi.class);
        }
        return slackApi;
    }
}
