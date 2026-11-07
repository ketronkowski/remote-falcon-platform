package com.remotefalcon.rules;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * A location code that changes every day at midnight in the show's time zone,
 * so a code shared with live-stream viewers stops working the next day.
 *
 * <p>The day's code is {@code HMAC-SHA256(secret, "<showSubdomain>:<yyyy-MM-dd>")},
 * the first four bytes read as an unsigned big-endian number, then
 * {@code 1000 + (n % 9000)}: always four digits. Anything holding the same secret
 * (for example the live-stream page on another host) works out the same code on
 * its own, so nothing has to copy it between systems.
 *
 * <p>Turned on by setting {@code remotefalcon.daily-code.secret}
 * (env {@code REMOTEFALCON_DAILY_CODE_SECRET}). The zone is
 * {@code remotefalcon.daily-code.zone} (default {@code America/New_York}). While it
 * is on, the daily code replaces the fixed {@code locationCode} for CODE and
 * GEO_OR_CODE shows. When it is off, nothing changes.
 */
public final class DailyCode {
  private static final Logger LOG = Logger.getLogger(DailyCode.class);
  static final String SECRET_KEY = "remotefalcon.daily-code.secret";
  static final String ZONE_KEY = "remotefalcon.daily-code.zone";
  static final String DEFAULT_ZONE = "America/New_York";

  private final byte[] secret;
  private final ZoneId zone;
  private final Clock clock;

  DailyCode(String secret, ZoneId zone, Clock clock) {
    this.secret = secret == null || secret.isBlank() ? null : secret.getBytes(StandardCharsets.UTF_8);
    this.zone = zone;
    this.clock = clock;
  }

  /** Daily code turned off. */
  static DailyCode off() {
    return new DailyCode(null, ZoneId.of(DEFAULT_ZONE), Clock.systemUTC());
  }

  /** Reads the secret and zone from config; any problem leaves the daily code off. */
  public static DailyCode fromConfig() {
    try {
      var config = ConfigProvider.getConfig();
      Optional<String> secret = config.getOptionalValue(SECRET_KEY, String.class);
      String zone = config.getOptionalValue(ZONE_KEY, String.class).orElse(DEFAULT_ZONE);
      return new DailyCode(secret.orElse(null), ZoneId.of(zone), Clock.systemUTC());
    } catch (RuntimeException e) {
      LOG.warnf("Daily location code is off: %s", e.getMessage());
      return off();
    }
  }

  public boolean enabled() {
    return secret != null;
  }

  /** The code for one show on one calendar day. Only call when {@link #enabled()}. */
  int codeFor(String showSubdomain, LocalDate date) {
    String message = showSubdomain.toLowerCase(Locale.ROOT) + ":" + date;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] h = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
      long n = ((h[0] & 0xFFL) << 24) | ((h[1] & 0xFFL) << 16) | ((h[2] & 0xFFL) << 8) | (h[3] & 0xFFL);
      return (int) (1000 + (n % 9000));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 unavailable", e);
    }
  }

  /** True when {@code entered} is today's code for this show. */
  public boolean matches(String showSubdomain, Integer entered) {
    if (!enabled() || showSubdomain == null || entered == null) {
      return false;
    }
    return codeFor(showSubdomain, LocalDate.now(clock.withZone(zone))) == entered;
  }
}
