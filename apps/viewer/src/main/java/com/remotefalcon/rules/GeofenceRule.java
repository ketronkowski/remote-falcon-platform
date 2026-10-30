package com.remotefalcon.rules;

import com.remotefalcon.library.enums.LocationCheckMethod;
import com.remotefalcon.library.enums.StatusResponse;
import com.remotefalcon.library.models.GpsLocation;
import com.remotefalcon.library.models.Preference;
import com.remotefalcon.util.LocationUtil;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Enforces the show's "check viewer present" setting ({@code locationCheckMethod}):
 * <ul>
 *   <li>GEO: denies a viewer outside the allowed radius (or with missing
 *   coordinates). A viewer is allowed if within {@code allowedRadius} of the
 *   primary location OR any of {@code additionalGpsLocations} (#16).</li>
 *   <li>CODE: denies a viewer who did not enter the show's {@code locationCode}.</li>
 *   <li>GEO_OR_CODE: allows a viewer who entered the right code from anywhere
 *   (e.g. watching a live stream), otherwise falls back to the GEO check.</li>
 * </ul>
 * Skips when the check is off. The code is compared here rather than in the
 * browser, and the viewer getShow projection leaves it out.
 */
public final class GeofenceRule implements Rule {
  private static final Logger LOG = Logger.getLogger(GeofenceRule.class);

  @Override
  public Decision evaluate(EvaluationContext ctx) {
    var preferences = ctx.show().getPreferences();
    LocationCheckMethod method = preferences.getLocationCheckMethod();
    if (method == LocationCheckMethod.CODE) {
      return codeMatches(preferences, ctx.locationCode())
          ? Decision.allow()
          : Decision.deny(StatusResponse.INVALID_CODE.name());
    }
    if (method == LocationCheckMethod.GEO_OR_CODE) {
      if (ctx.locationCode() != null) {
        return codeMatches(preferences, ctx.locationCode())
            ? Decision.allow()
            : Decision.deny(StatusResponse.INVALID_CODE.name());
      }
      return evaluateGeo(ctx, preferences);
    }
    if (method != LocationCheckMethod.GEO) {
      return Decision.skip();
    }
    return evaluateGeo(ctx, preferences);
  }

  private static Decision evaluateGeo(EvaluationContext ctx, Preference preferences) {
    if (ctx.latitude() == null || ctx.longitude() == null) {
      return Decision.deny(StatusResponse.INVALID_LOCATION.name());
    }
    if (preferences.getAllowedRadius() == null) {
      LOG.errorf("GPS check enabled but allowedRadius is null for show: %s", ctx.show().getShowSubdomain());
      return Decision.deny(StatusResponse.INVALID_LOCATION.name());
    }
    Float radius = preferences.getAllowedRadius();
    if (within(preferences.getShowLatitude(), preferences.getShowLongitude(), radius, ctx.latitude(), ctx.longitude())) {
      return Decision.allow();
    }
    List<GpsLocation> additional = preferences.getAdditionalGpsLocations();
    if (additional != null) {
      for (GpsLocation location : additional) {
        if (within(location.getLatitude(), location.getLongitude(), radius, ctx.latitude(), ctx.longitude())) {
          return Decision.allow();
        }
      }
    }
    return Decision.deny(StatusResponse.INVALID_LOCATION.name());
  }

  /** A show with no code set (null or 0) never matches, so CODE mode fails closed. */
  private static boolean codeMatches(Preference preferences, Integer entered) {
    Integer code = preferences.getLocationCode();
    return code != null && code > 0 && code.equals(entered);
  }

  /** True when the viewer point is within {@code radius} miles of the center. Null-center safe. */
  private static boolean within(Float centerLat, Float centerLon, Float radius, Float lat, Float lon) {
    if (centerLat == null || centerLon == null) {
      return false;
    }
    return LocationUtil.asTheCrowFlies(centerLat, centerLon, lat, lon) <= radius;
  }
}
