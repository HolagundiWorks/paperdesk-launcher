package `in`.hcworks.paperdesk.launcher

import android.graphics.drawable.Drawable

data class LauncherApp(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)

object LauncherAppSorter {
    private val pinnedOrder = listOf(
        "in.hcworks.madhvanidhi",
        "in.hcworks.architectcalculator",
        "in.hcworks.pomodoro"
    )

    fun sort(apps: List<LauncherApp>): List<LauncherApp> = apps.sortedWith(
        compareBy<LauncherApp> {
            val index = pinnedOrder.indexOf(it.packageName)
            if (index == -1) Int.MAX_VALUE else index
        }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    )
}
