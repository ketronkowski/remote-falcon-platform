package com.remotefalcon.plugins.api.filters;

import com.remotefalcon.plugins.api.cache.EmptyPollCache;
import com.remotefalcon.plugins.api.cache.EmptyPollCache.PollKind;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Drops a show's cached "queue empty" / "no votes" answers after any
 * non-poll plugin call for that show has run.
 *
 * <p>Any of them can write requests or votes from this pod
 * (updateWhatsPlaying injects managed PSAs; purgeQueue, resetAllVotes and
 * the viewer-control toggles rewrite them), so the next poll goes back to
 * the DB instead of waiting out the TTL. It runs on the response, after the
 * write, so a poll racing the write can at worst re-cache the pre-write
 * state for one TTL, the same bound as a write from another service.
 */
@Provider
public class PollCacheInvalidationFilter implements ContainerResponseFilter {

  @Inject
  EmptyPollCache emptyPollCache;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    String path = requestContext.getUriInfo().getPath();
    if (PollKind.fromPath(path) != null) {
      return;
    }
    // GET /queue only reads the queue (dashboards poll it every few seconds),
    // so it must not wipe the empty-poll cache the way a write does.
    if (path != null && path.endsWith("/queue")) {
      return;
    }
    String showToken = ShowTokenFilter.showToken(requestContext);
    if (showToken != null && !showToken.isEmpty()) {
      emptyPollCache.invalidate(showToken);
    }
  }
}
