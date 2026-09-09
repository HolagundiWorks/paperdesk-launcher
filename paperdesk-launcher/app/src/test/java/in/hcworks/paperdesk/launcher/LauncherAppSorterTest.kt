package `in`.hcworks.paperdesk.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherAppSorterTest {
    @Test fun paperdeskAppsAppearFirstInExpectedOrder() {
        val apps = listOf(
            LauncherApp("Notes", "org.example.notes"),
            LauncherApp("Pomodoro", "in.hcworks.pomodoro"),
            LauncherApp("Calculator", "in.hcworks.architectcalculator"),
            LauncherApp("Madhvanidhi", "in.hcworks.madhvanidhi"),
            LauncherApp("Calendar", "org.example.calendar")
        )

        assertEquals(
            listOf(
                "in.hcworks.madhvanidhi",
                "in.hcworks.architectcalculator",
                "in.hcworks.pomodoro",
                "org.example.calendar",
                "org.example.notes"
            ),
            LauncherAppSorter.sort(apps).map { it.packageName }
        )
    }
}
