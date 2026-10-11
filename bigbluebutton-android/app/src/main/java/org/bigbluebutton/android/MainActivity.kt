package org.bigbluebutton.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bigbluebutton.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        b.inputLink.setText(prefs.link)
        b.inputServer.setText(prefs.server)
        b.inputSecret.setText(prefs.secret)
        b.inputMeetingId.setText(prefs.meetingId)
        b.inputMeetingName.setText(prefs.meetingName)
        b.inputName.setText(prefs.userName)
        b.toggleRole.check(if (prefs.moderator) R.id.btnRoleModerator else R.id.btnRoleParticipant)
        b.toggleMode.check(if (prefs.serverMode) R.id.btnModeServer else R.id.btnModeLink)
        applyMode(prefs.serverMode)

        b.toggleMode.addOnButtonCheckedListener { _, id, checked ->
            if (checked) applyMode(id == R.id.btnModeServer)
        }
        b.btnJoin.setOnClickListener { join() }
    }

    private fun applyMode(server: Boolean) {
        b.layoutServer.visibility = if (server) View.VISIBLE else View.GONE
        b.layoutLink.visibility = if (server) View.GONE else View.VISIBLE
        showError(null)
    }

    private fun showError(msg: String?) {
        b.textError.text = msg
        b.textError.visibility = if (msg == null) View.GONE else View.VISIBLE
    }

    private fun join() {
        showError(null)
        if (b.toggleMode.checkedButtonId == R.id.btnModeLink) {
            val link = BbbApi.normalizeLink(b.inputLink.text.toString())
            if (link == null) {
                showError(getString(R.string.err_link))
                return
            }
            prefs.link = link
            prefs.serverMode = false
            openMeeting(link, getString(R.string.app_name))
            return
        }

        val server = b.inputServer.text.toString().trim()
        val secret = b.inputSecret.text.toString().trim()
        val meetingId = b.inputMeetingId.text.toString().trim()
        val meetingName = b.inputMeetingName.text.toString().trim()
        val name = b.inputName.text.toString().trim()
        val moderator = b.toggleRole.checkedButtonId == R.id.btnRoleModerator
        if (server.isEmpty() || secret.isEmpty() || meetingId.isEmpty() || name.isEmpty()) {
            showError(getString(R.string.err_fields))
            return
        }
        prefs.server = server
        prefs.secret = secret
        prefs.meetingId = meetingId
        prefs.meetingName = meetingName
        prefs.userName = name
        prefs.moderator = moderator
        prefs.serverMode = true

        setBusy(true)
        lifecycleScope.launch {
            try {
                val url = withContext(Dispatchers.IO) {
                    BbbApi.createAndJoin(
                        server, secret, meetingId, meetingName, name,
                        if (moderator) BbbApi.Role.MODERATOR else BbbApi.Role.VIEWER,
                    )
                }
                openMeeting(url, meetingName.ifBlank { meetingId })
            } catch (e: BbbApi.ApiException) {
                showError(getString(R.string.err_api, e.message))
            } catch (e: Exception) {
                showError(getString(R.string.err_network, e.message ?: e.javaClass.simpleName))
            } finally {
                setBusy(false)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        b.btnJoin.isEnabled = !busy
        b.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
    }

    private fun openMeeting(url: String, title: String) {
        startActivity(
            Intent(this, MeetingActivity::class.java)
                .putExtra(MeetingActivity.EXTRA_URL, url)
                .putExtra(MeetingActivity.EXTRA_TITLE, title)
        )
    }
}
