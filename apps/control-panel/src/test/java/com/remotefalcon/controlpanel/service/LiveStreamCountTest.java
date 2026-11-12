package com.remotefalcon.controlpanel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.remotefalcon.library.models.Stat;
import java.util.List;
import org.junit.jupiter.api.Test;

class LiveStreamCountTest {

  private static Stat.Jukebox request(String via) {
    return Stat.Jukebox.builder().name("Song").via(via).build();
  }

  @Test
  void countsOnlyRequestsMadeWithTheCode() {
    assertEquals(2, DashboardService.countLiveStream(
        List.of(request("CODE"), request("GPS"), request(null), request("CODE"))));
    assertEquals(0, DashboardService.countLiveStream(List.of()));
  }
}
