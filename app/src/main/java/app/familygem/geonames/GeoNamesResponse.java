package app.familygem.geonames;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class GeoNamesResponse {
    @SerializedName("geonames")
    public List<GeoNamesToponym> geonames;
}
