package com.remotefalcon.plugins.api.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-only view of the request queue, for show dashboards. Unlike
 * /nextPlaylistInQueue (which FPP calls and which REMOVES the head of the
 * queue), reading this never changes anything.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueueResponse {
  private String playingNow;
  private String playingNext;
  private List<Item> requests;

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Item {
    private Integer position;
    /** FPP sequence name. */
    private String sequence;
    /** Name viewers see (falls back to the sequence name). */
    private String displayName;
    /** True for owner-queued items (tests, managed PSAs). */
    private Boolean ownerRequested;
    /** False for PSAs and other non-song items FPP still plays. */
    private Boolean song;
  }
}
