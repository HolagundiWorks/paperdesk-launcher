package `in`.hcworks.paperdesk.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var appList: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var apps: InstalledApps
    private lateinit var defaultAction: TextView

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateDefaultLauncherState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apps = InstalledApps(this)
        adapter = AppListAdapter { app ->
            if (!apps.launch(app)) Toast.makeText(this, R.string.app_unavailable, Toast.LENGTH_SHORT).show()
        }
        appList = findViewById(R.id.appList)
        appList.layoutManager = LinearLayoutManager(this)
        appList.adapter = adapter

        defaultAction = findViewById(R.id.defaultLauncherAction)
        defaultAction.setOnClickListener { requestDefaultLauncher() }
        findViewById<View>(R.id.openMadhvanidhi).setOnClickListener { openPackage("in.hcworks.madhvanidhi") }
        findViewById<View>(R.id.openCalculator).setOnClickListener { openPackage("in.hcworks.architectcalculator") }
        findViewById<View>(R.id.openPomodoro).setOnClickListener { openPackage("in.hcworks.pomodoro") }
    }

    override fun onResume() {
        super.onResume()
        adapter.submit(apps.load())
        updateDefaultLauncherState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appList.scrollToPosition(0)
    }

    @Deprecated("The launcher owns the root navigation destination")
    override fun onBackPressed() {
        appList.smoothScrollToPosition(0)
    }

    private fun openPackage(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) {
            Toast.makeText(this, R.string.app_not_installed, Toast.LENGTH_SHORT).show()
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startActivity(launch)
    }

    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            roleRequest.launch(Intent(Settings.ACTION_HOME_SETTINGS))
        }
    }

    private fun updateDefaultLauncherState() {
        val isDefault = if (Build.VERSION.SDK_INT >= 29) {
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        } else {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.resolveActivity(home, 0)?.activityInfo?.packageName == packageName
        }
        defaultAction.text = getString(if (isDefault) R.string.default_launcher_active else R.string.make_default)
        defaultAction.isEnabled = !isDefault
        defaultAction.alpha = if (isDefault) 0.62f else 1f
    }
}
