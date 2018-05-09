package id.web.owlstudio.googlemapsapi.network;

import id.web.owlstudio.googlemapsapi.network.response.ResponseRoute;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiServices {
    // https://maps.googleapis.com/maps/api/directions/ <-- BASE_URL
    // json? <-- disebut END POINT
    // origin=75+9th+Ave+New+York,+NY <-- disebut Parameter
    // &destination=MetLife+Stadium+1+MetLife+Stadium+Dr+East+Rutherford,+NJ+07073 <-- disebut Parameter
    // &key=AIzaSyCmYliAfT87YdcgObVGXDYPPy1_e_I0ugc <-- disebut Parameter

    @GET ("json")
    Call<ResponseRoute> request_route(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("mode") String mode,
            @Query("avoid") String avoid,
            @Query("key") String key
    );
}
