package io.intrepid.russell.checkin;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface SlackApi {
    String BASE_URL = "https://hooks.slack.com/services/";

    @Headers("Content-type: application/json")
    @POST("T026B13VA/B0J6ASUNS/nr4GlZazBh1Iq1cFhgMA4BGD")
    Call<Void> postCheckIn(@Body TextRequest request);

    class TextRequest {
        String text;

        public TextRequest(String text) {
            this.text = text;
        }
    }
}
