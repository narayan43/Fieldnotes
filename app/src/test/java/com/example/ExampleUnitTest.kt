package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun timeFormatter_humanDuration() {
    assertEquals("0m", com.example.util.TimeFormatter.formatHumanDuration(0))
    assertEquals("< 1m", com.example.util.TimeFormatter.formatHumanDuration(30000))
    assertEquals("12m", com.example.util.TimeFormatter.formatHumanDuration(12 * 60 * 1000L))
    assertEquals("2h 40m", com.example.util.TimeFormatter.formatHumanDuration((2 * 60 + 40) * 60 * 1000L))
  }

  @Test
  fun timeFormatter_timerClock() {
    assertEquals("00:05", com.example.util.TimeFormatter.formatTimerClock(5000))
    assertEquals("12:34", com.example.util.TimeFormatter.formatTimerClock((12 * 60 + 34) * 1000L))
    assertEquals("01:02:03", com.example.util.TimeFormatter.formatTimerClock(((1 * 60 + 2) * 60 + 3) * 1000L))
  }
}
