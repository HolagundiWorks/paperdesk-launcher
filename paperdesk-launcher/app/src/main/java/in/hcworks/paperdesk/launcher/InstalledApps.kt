package `in`.hcworks.paperdesk.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class InstalledApps(private val context: Context) {
    fun load(): List<LauncherApp> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(
                query,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(query, PackageManager.MATCH_ALL)
        }

        val apps = resolved
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                LauncherApp(
                    label = it.loadLabel(context.packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(context.packageManager)
                )
            }
            .toList()
        return LauncherAppSorter.sort(apps)
    }

    fun launch(app: LauncherApp): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        return true
    }
}
