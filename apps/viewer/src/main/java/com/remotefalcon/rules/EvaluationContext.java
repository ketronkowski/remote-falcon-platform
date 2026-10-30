package com.remotefalcon.rules;

import com.remotefalcon.library.util.IpMatcher;

import com.remotefalcon.library.quarkus.entity.Show;

/**
 * Inputs shared by every vote/request enforcement rule, resolved once per
 * mutation and passed down the chain (PRD-009, ADR-4).
 *
 * <p>{@code votesInWindow} is the voter's vote count for the current show
 * session (votes since {@code Preference.votingWindowStartedAt}, #162), computed
 * by the service from the voteEvent collection — and only when a daily limit is
 * configured, so the I/O stays out of the rules and the rules remain pure. It is
 * {@code null} when no daily-cap check is needed.
 *
 * <p>{@code locationCode} is the code the viewer typed on the viewer page, or
 * {@code null} when they entered none. Only CODE and GEO_OR_CODE shows read it.
 */
public record EvaluationContext(Show show, String ip, String viewerId, Float latitude, Float longitude,
                                Integer locationCode, Long votesInWindow) {

  /** For callers that have no viewer-entered location code. */
  public EvaluationContext(Show show, String ip, String viewerId, Float latitude, Float longitude,
                           Long votesInWindow) {
    this(show, ip, viewerId, latitude, longitude, null, votesInWindow);
  }

  /**
   * #156 — true when this IP is on the show's voting-exempt allowlist (e.g. a
   * fixed lawn kiosk), so the per-voter rate limits don't apply to it.
   */
  public boolean votingExempt() {
    // Entries may be single addresses, CIDR blocks or ranges (#175).
    return IpMatcher.matchesAny(show.getPreferences().getVotingExemptIps(), ip);
  }
}
