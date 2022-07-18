package app.thecity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import app.thecity.data.SharedPref
import android.os.Bundle
import android.view.View
import app.thecity.utils.Tools
import app.thecity.utils.PermissionUtil
import app.thecity.ActivityMain
import java.util.*

class ActivitySplash : AppCompatActivity() {
    private var sharedPref: SharedPref? = null
    private var parent_view: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        parent_view = findViewById(R.id.parent_view)
        sharedPref = SharedPref(this)
        parent_view!!.setBackgroundColor(sharedPref!!.themeColorInt)

        // permission checker for android M or higher
        if (Tools.needRequestPermission()) {
            val permission = PermissionUtil.getDeniedPermission(this)
            if (permission.size != 0) {
                requestPermissions(permission, 200)
            } else {
                startActivityMainDelay()
            }
        } else {
            startActivityMainDelay()
        }

        // for system bar in lollipop
        Tools.systemBarLolipop(this)
    }

    private fun startActivityMainDelay() {
        // Show splash screen for 2 seconds
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                val i = Intent(this@ActivitySplash, ActivityMain::class.java)
                startActivity(i)
                finish() // kill current activity
            }
        }
        Timer().schedule(task, 2000)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 200) {
            for (perm in permissions) {
                val rationale = shouldShowRequestPermissionRationale(perm)
                sharedPref!!.setNeverAskAgain(perm, !rationale)
            }
            startActivityMainDelay()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}