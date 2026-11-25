package com.remotefalcon.service;

import com.remotefalcon.library.models.Stat;
import com.remotefalcon.library.quarkus.entity.Show;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Burst PSA cadence (first = 3, every 5 after, a 5-minute gap starts a new burst).
 * Plain unit tests: no Quarkus context needed.
 */
class PsaBurstCadenceTest {

  private static final Duration GAP = Duration.ofMinutes(5);
  private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 26, 18, 0);

  /** request number n (1-based) of a burst with one request per minute; returns whether a PSA follows it */
  private static boolean due(int n) {
    List<LocalDateTime> earlier = new ArrayList<>();
    for (int i = 0; i < n - 1; i++) {
      earlier.add(T0.plusMinutes(i));
    }
    return GraphQLMutationService.isPsaDueInBurst(earlier, T0.plusMinutes(n - 1), 3, 5, GAP);
  }

  @Test
  @DisplayName("PSA after the 3rd request, then every 5th: 3, 8, 13, 18")
  void cadenceInOneBurst() {
    List<Integer> fired = new ArrayList<>();
    for (int n = 1; n <= 20; n++) {
      if (due(n)) {
        fired.add(n);
      }
    }
    assertEquals(List.of(3, 8, 13, 18), fired);
  }

  @Test
  @DisplayName("A 5-minute break starts a new burst: the 3rd request after it gets the PSA again")
  void gapResetsBurst() {
    // 10 requests one minute apart (PSAs after 3 and 8), then a 6-minute break
    List<LocalDateTime> earlier = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      earlier.add(T0.plusMinutes(i));
    }
    LocalDateTime afterBreak = T0.plusMinutes(9 + 6);
    assertFalse(GraphQLMutationService.isPsaDueInBurst(earlier, afterBreak, 3, 5, GAP), "1st after the break");
    earlier.add(afterBreak);
    assertFalse(GraphQLMutationService.isPsaDueInBurst(earlier, afterBreak.plusMinutes(1), 3, 5, GAP), "2nd after the break");
    earlier.add(afterBreak.plusMinutes(1));
    assertTrue(GraphQLMutationService.isPsaDueInBurst(earlier, afterBreak.plusMinutes(2), 3, 5, GAP), "3rd after the break");
  }

  @Test
  @DisplayName("A gap just under 5 minutes keeps the burst going; exactly 5 minutes breaks it")
  void gapBoundary() {
    List<LocalDateTime> two = List.of(T0, T0.plusMinutes(1));
    assertTrue(GraphQLMutationService.isPsaDueInBurst(two, T0.plusMinutes(1).plus(Duration.ofSeconds(299)), 3, 5, GAP));
    assertFalse(GraphQLMutationService.isPsaDueInBurst(two, T0.plusMinutes(6), 3, 5, GAP));
  }

  @Test
  @DisplayName("Frequency 0 = only the first PSA of each burst")
  void zeroFrequencyOnlyFirst() {
    List<LocalDateTime> earlier = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      earlier.add(T0.plusSeconds(30L * i));
    }
    int fired = 0;
    for (int n = 1; n <= 12; n++) {
      if (GraphQLMutationService.isPsaDueInBurst(earlier.subList(0, n - 1), earlier.get(n - 1), 3, 0, GAP)) {
        fired++;
      }
    }
    assertEquals(1, fired);
  }

  @Test
  @DisplayName("psaDue: burst mode reads the show's jukebox stats; off = daily every-Nth cadence")
  void psaDueUsesStatsAndMode() {
    Show show = mock(Show.class, RETURNS_DEEP_STUBS);
    when(show.getPreferences().getPsaFrequency()).thenReturn(5);
    List<Stat.Jukebox> stats = new ArrayList<>();
    stats.add(Stat.Jukebox.builder().dateTime(T0).name("a").build());
    stats.add(Stat.Jukebox.builder().dateTime(T0.plusMinutes(1)).name("b").build());
    when(show.getStats().getJukebox()).thenReturn(stats);

    GraphQLMutationService service = new GraphQLMutationService();
    service.psaBurstEnabled = true;
    service.psaBurstFirst = 3;
    service.psaBurstGap = GAP;
    assertTrue(service.psaDue(show, T0.plusMinutes(2)), "3rd request of the burst");

    service.psaBurstEnabled = false;
    assertFalse(service.psaDue(show, T0.plusMinutes(2)), "daily mode: 3rd request of the day, frequency 5");
  }
}
