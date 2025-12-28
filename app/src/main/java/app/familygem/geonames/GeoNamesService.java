package app.familygem.geonames;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeoNamesService {
    @GET("searchJSON")
    Call<GeoNamesResponse> search(
            @Query("name_startsWith") String nameStartsWith,
            @Query("maxRows") int maxRows,
            @Query("style") String style,
            @Query("lang") String lang,
            @Query("username") String username);
}
