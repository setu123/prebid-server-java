package org.prebid.server.proto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor(staticName = "of")
@Value
public class UsersyncInfo {

    String url;

    String type;

    @JsonProperty("supportCORS")
    Boolean supportCORS;
}