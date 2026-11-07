package com.remotefalcon.rules;

import com.remotefalcon.library.enums.LocationCheckMethod;
import com.remotefalcon.library.enums.StatusResponse;
import com.remotefalcon.library.models.Preference;
import com.remotefalcon.library.quarkus.entity.Show;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyCodeTest {
  private static final ZoneId EASTERN = ZoneId.of("America/New_York");

  /** 2026-11-07 21:00 Eastern (EST, UTC-5) = 2026-11-08 02:00 UTC. */
  private static final Instant SHOW_TIME = Instant.parse("2026-11-08T02:00:00Z");

  private static DailyCode at(Instant instant) {
    return new DailyCode("test-secret", EASTERN, Clock.fixed(instant, ZoneId.of("UTC")));
  }

  // Reference values, also produced by:
  // python3 -c "import hmac,hashlib; h=hmac.new(b'test-secret',b'test-show:2026-11-07',hashlib.sha256).digest(); print(1000+int.from_bytes(h[:4],'big')%9000)"
  @Test
  void codeFor_matchesTheDocumentedAlgorithm() {
    DailyCode dc = at(SHOW_TIME);
    assertEquals(1649, dc.codeFor("test-show", LocalDate.of(2026, 11, 7)));
    assertEquals(2395, dc.codeFor("test-show", LocalDate.of(2026, 11, 8)));
    assertEquals(1649, dc.codeFor("Test-Show", LocalDate.of(2026, 11, 7)), "subdomain is case-insensitive");
  }

  @Test
  void matches_usesTheShowsLocalDateNotUtc() {
    // UTC is already Nov 8 at show time, but it's still Nov 7 in Eastern time.
    DailyCode dc = at(SHOW_TIME);
    assertTrue(dc.matches("test-show", 1649));
    assertFalse(dc.matches("test-show", 2395));
  }

  @Test
  void matches_rollsOverAtLocalMidnight() {
    assertTrue(at(Instant.parse("2026-11-08T04:59:59Z")).matches("test-show", 1649)); // 23:59:59 EST Nov 7
    assertTrue(at(Instant.parse("2026-11-08T05:00:00Z")).matches("test-show", 2395)); // 00:00 EST Nov 8
    assertFalse(at(Instant.parse("2026-11-08T05:00:00Z")).matches("test-show", 1649));
  }

  @Test
  void off_whenSecretMissingOrBlank() {
    assertFalse(DailyCode.off().enabled());
    assertFalse(new DailyCode("  ", EASTERN, Clock.systemUTC()).enabled());
    assertFalse(DailyCode.off().matches("test-show", 1649));
  }

  @Test
  void matches_rejectsNulls() {
    DailyCode dc = at(SHOW_TIME);
    assertFalse(dc.matches("test-show", null));
    assertFalse(dc.matches(null, 1649));
  }

  // --- GeofenceRule with the daily code on ----------------------------------

  private static Show show(LocationCheckMethod method, Integer fixedCode) {
    Preference p = new Preference();
    p.setLocationCheckMethod(method);
    p.setLocationCode(fixedCode);
    p.setAllowedRadius(0.5f);
    p.setShowLatitude(42.808071f);
    p.setShowLongitude(-71.672248f);
    Show show = new Show();
    show.setShowSubdomain("test-show");
    show.setPreferences(p);
    show.setVotes(new ArrayList<>());
    show.setRequests(new ArrayList<>());
    return show;
  }

  private static EvaluationContext ctx(Show show, Float lat, Float lon, Integer code) {
    return new EvaluationContext(show, "1.2.3.4", null, lat, lon, code, null);
  }

  @Test
  void geoOrCode_allowsTodaysCodeFromFarAway() {
    GeofenceRule rule = new GeofenceRule(at(SHOW_TIME));
    assertFalse(rule.evaluate(ctx(show(LocationCheckMethod.GEO_OR_CODE, null), 51.5074f, -0.1278f, 1649)).denied());
  }

  @Test
  void geoOrCode_rejectsYesterdaysCodeAndTheFixedCode() {
    GeofenceRule rule = new GeofenceRule(at(Instant.parse("2026-11-09T02:00:00Z"))); // Nov 8 Eastern
    Show show = show(LocationCheckMethod.GEO_OR_CODE, 4321);
    Decision yesterday = rule.evaluate(ctx(show, null, null, 1649));
    assertTrue(yesterday.denied());
    assertEquals(StatusResponse.INVALID_CODE.name(), yesterday.reason());
    assertTrue(rule.evaluate(ctx(show, null, null, 4321)).denied(), "fixed code is replaced while the daily code is on");
  }

  @Test
  void geoOrCode_withNoCodeStillUsesGps() {
    GeofenceRule rule = new GeofenceRule(at(SHOW_TIME));
    Show show = show(LocationCheckMethod.GEO_OR_CODE, null);
    assertFalse(rule.evaluate(ctx(show, 42.8081f, -71.6722f, null)).denied());
    assertTrue(rule.evaluate(ctx(show, 51.5074f, -0.1278f, null)).denied());
  }

  @Test
  void code_acceptsOnlyTodaysCode() {
    GeofenceRule rule = new GeofenceRule(at(SHOW_TIME));
    Show show = show(LocationCheckMethod.CODE, 4321);
    assertFalse(rule.evaluate(ctx(show, null, null, 1649)).denied());
    assertTrue(rule.evaluate(ctx(show, null, null, 4321)).denied());
  }

  @Test
  void dailyCodeOff_keepsTheFixedCode() {
    GeofenceRule rule = new GeofenceRule(DailyCode.off());
    Show show = show(LocationCheckMethod.GEO_OR_CODE, 4321);
    assertFalse(rule.evaluate(ctx(show, null, null, 4321)).denied());
    assertTrue(rule.evaluate(ctx(show, null, null, 1649)).denied());
  }
}
