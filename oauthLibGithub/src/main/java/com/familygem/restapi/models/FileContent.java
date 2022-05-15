
package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FileContent {

    @SerializedName("content")
    @Expose
    public Content content;
    @SerializedName("commit")
    @Expose
    public Commit commit;

}
