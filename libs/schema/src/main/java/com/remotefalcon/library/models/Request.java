package com.remotefalcon.library.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.graphql.Type;

@Type
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {
    private Sequence sequence;
    private Integer position;
    private String viewerRequested;
    private Boolean ownerRequested;
    // How a viewer request passed the "check viewer present" setting: GPS
    // (driveway) or CODE (the live-stream page sends the daily code), as on
    // Stat.Jukebox. Null for owner/test requests, PSAs, leaders, when the check
    // is off, and on rows queued before this field existed.
    private String via;
}
