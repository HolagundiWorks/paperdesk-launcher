package `in`.hcworks.paperdesk.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val onOpen: (LauncherApp) -> Unit
) : RecyclerView.Adapter<AppListAdapter.Holder>() {
    private val apps = mutableListOf<LauncherApp>()

    fun submit(items: List<LauncherApp>) {
        apps.clear()
        apps.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(apps[position])

    override fun getItemCount(): Int = apps.size

    inner class Holder(private val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        private val icon: ImageView = root.findViewById(R.id.appIcon)
        private val label: TextView = root.findViewById(R.id.appLabel)

        fun bind(app: LauncherApp) {
            icon.setImageDrawable(app.icon)
            label.text = app.label
            root.contentDescription = app.label
            root.setOnClickListener { onOpen(app) }
        }
    }
}
