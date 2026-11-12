package com.remotefalcon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.remotefalcon.library.enums.LocationCheckMethod;
import com.remotefalcon.library.models.Preference;
import com.remotefalcon.library.quarkus.entity.Show;
import org.junit.jupiter.api.Test;

class RequestViaTest {

  private static Show show(LocationCheckMethod method) {
    Show show = new Show();
    Preference preferences = new Preference();
    preferences.setLocationCheckMethod(method);
    show.setPreferences(preferences);
    return show;
  }

  @Test
  void recordsHowTheRequestPassedTheLocationCheck() {
    assertEquals("GPS", GraphQLMutationService.requestVia(show(LocationCheckMethod.GEO), null));
    assertEquals("CODE", GraphQLMutationService.requestVia(show(LocationCheckMethod.CODE), 1234));
    assertEquals("CODE", GraphQLMutationService.requestVia(show(LocationCheckMethod.GEO_OR_CODE), 1234));
    assertEquals("GPS", GraphQLMutationService.requestVia(show(LocationCheckMethod.GEO_OR_CODE), null));
    assertNull(GraphQLMutationService.requestVia(show(LocationCheckMethod.NONE), null));
    assertNull(GraphQLMutationService.requestVia(new Show(), null));
  }
}
