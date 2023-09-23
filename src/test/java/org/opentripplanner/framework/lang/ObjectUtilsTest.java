package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ObjectUtilsTest {

  @Test
  void ifNotNull() {
    assertEquals("exp", ObjectUtils.ifNotNull("exp", "default"));
    assertEquals("default", ObjectUtils.ifNotNull(null, "default"));
    assertNull(ObjectUtils.ifNotNull(null, null));
  }

  @Test
  void ifNotNullFunctional() {
    var i = new IntBox(5);
    assertEquals(5, ObjectUtils.ifNotNull(i, IntBox::get, 7));
    assertEquals(7, ObjectUtils.ifNotNull(null, IntBox::get, 7));

    // Default is null
    assertEquals(5, ObjectUtils.ifNotNull(i, IntBox::get, null));
    assertNull(ObjectUtils.ifNotNull(null, IntBox::get, null));

    var o = new AtomicReference<String>(null);
    assertEquals("DEFAULT", ObjectUtils.ifNotNull(o, AtomicReference::get, "DEFAULT"));
    assertNull(ObjectUtils.ifNotNull(o, AtomicReference::get, null));
  }

  @Test
  void requireNotInitialized() {
    assertEquals("new", ObjectUtils.requireNotInitialized(null, "new"));
    assertThrows(
      IllegalStateException.class,
      () -> ObjectUtils.requireNotInitialized("old", "new")
    );
  }

  @Test
  void toStringTest() {
    assertEquals("1", ObjectUtils.toString(1));
    assertEquals("", ObjectUtils.toString(null));
  }
}
