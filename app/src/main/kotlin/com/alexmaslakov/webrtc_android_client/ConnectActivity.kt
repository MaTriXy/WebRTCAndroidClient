/*
 * libjingle
 * Copyright 2014 Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.alexmaslakov.webrtc_android_client

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.*

import org.json.JSONArray
import org.json.JSONException

import java.util.ArrayList
import java.util.Random

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class ConnectActivity : Activity() {

  private var addRoomButton: ImageButton? = null
  private var removeRoomButton: ImageButton? = null
  private var connectButton: ImageButton? = null
  private var connectLoopbackButton: ImageButton? = null
  private var roomEditText: EditText? = null
  private var roomListView: ListView? = null
  private var sharedPref: SharedPreferences? = null
  private var keyprefVideoCallEnabled: String? = null
  private var keyprefResolution: String? = null
  private var keyprefFps: String? = null
  private var keyprefVideoBitrateType: String? = null
  private var keyprefVideoBitrateValue: String? = null
  private var keyprefVideoCodec: String? = null
  private var keyprefAudioBitrateType: String? = null
  private var keyprefAudioBitrateValue: String? = null
  private var keyprefAudioCodec: String? = null
  private var keyprefHwCodecAcceleration: String? = null
  private var keyprefCpuUsageDetection: String? = null
  private var keyprefDisplayHud: String? = null
  private var keyprefRoomServerUrl: String? = null
  private var keyprefRoom: String? = null
  private var keyprefRoomList: String? = null
  private var roomList: ArrayList<String>? = null
  private var adapter: ArrayAdapter<String>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Get setting keys.
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    keyprefVideoCallEnabled = getString(R.string.pref_videocall_key)
    keyprefResolution = getString(R.string.pref_resolution_key)
    keyprefFps = getString(R.string.pref_fps_key)
    keyprefVideoBitrateType = getString(R.string.pref_startvideobitrate_key)
    keyprefVideoBitrateValue = getString(R.string.pref_startvideobitratevalue_key)
    keyprefVideoCodec = getString(R.string.pref_videocodec_key)
    keyprefHwCodecAcceleration = getString(R.string.pref_hwcodec_key)
    keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key)
    keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key)
    keyprefAudioCodec = getString(R.string.pref_audiocodec_key)
    keyprefCpuUsageDetection = getString(R.string.pref_cpu_usage_detection_key)
    keyprefDisplayHud = getString(R.string.pref_displayhud_key)
    keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key)
    keyprefRoom = getString(R.string.pref_room_key)
    keyprefRoomList = getString(R.string.pref_room_list_key)

    setContentView(R.layout.activity_connect)

    roomEditText = findViewById(R.id.room_edittext) as EditText
    roomEditText!!.setOnEditorActionListener(object : TextView.OnEditorActionListener {
      override fun onEditorAction(textView: TextView, i: Int, keyEvent: KeyEvent): Boolean {
        if (i == EditorInfo.IME_ACTION_DONE) {
          addRoomButton!!.performClick()
          return true
        }
        return false
      }
    })
    roomEditText!!.requestFocus()

    roomListView = findViewById(R.id.room_listview) as ListView
    roomListView!!.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE)

    addRoomButton = findViewById(R.id.add_room_button) as ImageButton
    addRoomButton!!.setOnClickListener(addRoomListener())
    removeRoomButton = findViewById(R.id.remove_room_button) as ImageButton
    removeRoomButton!!.setOnClickListener(removeRoomListener())
    connectButton = findViewById(R.id.connect_button) as ImageButton
    connectButton!!.setOnClickListener(connectListener)
    connectLoopbackButton = findViewById(R.id.connect_loopback_button) as ImageButton
    connectLoopbackButton!!.setOnClickListener(connectListener)

    // If an implicit VIEW intent is launching the app, go directly to that URL.
    val intent = getIntent()
    if ("android.intent.action.VIEW" == intent.getAction() && !commandLineRun) {
      commandLineRun = true
      val loopback = intent.getBooleanExtra(CallActivity.EXTRA_LOOPBACK, false)
      val runTimeMs = intent.getIntExtra(CallActivity.EXTRA_RUNTIME, 0)
      val room = sharedPref!!.getString(keyprefRoom, "")
      roomEditText!!.setText(room)
      connectToRoom(loopback, runTimeMs)
      return
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    getMenuInflater().inflate(R.menu.connect_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle presses on the action bar items.
    if (item.getItemId() == R.id.action_settings) {
      val intent = Intent(this, javaClass<SettingsActivity>())
      startActivity(intent)
      return true
    } else {
      return super.onOptionsItemSelected(item)
    }
  }

  override fun onPause() {
    super.onPause()
    val room = roomEditText!!.getText().toString()
    val roomListJson = JSONArray(roomList).toString()
    val editor = sharedPref!!.edit()
    editor.putString(keyprefRoom, room)
    editor.putString(keyprefRoomList, roomListJson)
    editor.commit()
  }

  override fun onResume() {
    super.onResume()
    val room = sharedPref!!.getString(keyprefRoom, "")
    roomEditText!!.setText(room)
    roomList = ArrayList<String>()
    val roomListJson = sharedPref!!.getString(keyprefRoomList, null)
    if (roomListJson != null) {
      try {
        val jsonArray = JSONArray(roomListJson)
        for (i in 0..jsonArray.length() - 1) {
          roomList!!.add(jsonArray.get(i).toString())
        }
      } catch (e: JSONException) {
        Log.e(TAG, "Failed to load room list: " + e.toString())
      }

    }
    adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, roomList)
    roomListView!!.setAdapter(adapter)
    if (adapter!!.getCount() > 0) {
      roomListView!!.requestFocus()
      roomListView!!.setItemChecked(0, true)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (requestCode == CONNECTION_REQUEST && commandLineRun) {
      Log.d(TAG, "Return: " + resultCode)
      setResult(resultCode)
      commandLineRun = false
      finish()
    }
  }

  private val connectListener = object : OnClickListener {
    override fun onClick(view: View) {
      var loopback = false
      if (view.getId() == R.id.connect_loopback_button) {
        loopback = true
      }
      commandLineRun = false
      connectToRoom(loopback, 0)
    }
  }

  private fun connectToRoom(loopback: Boolean, runTimeMs: Int) {
    // Get room name (random for loopback).
    var roomId: String?
    if (loopback) {
      roomId = Integer.toString((Random()).nextInt(100000000))
    } else {
      roomId = getSelectedItem()
      if (roomId == null) {
        roomId = roomEditText!!.getText().toString()
      }
    }

    val roomUrl = sharedPref!!.getString(keyprefRoomServerUrl, getString(R.string.pref_room_server_url_default))

    // Video call enabled flag.
    val videoCallEnabled = sharedPref!!.getBoolean(keyprefVideoCallEnabled!!,
      java.lang.Boolean.valueOf(getString(R.string.pref_videocall_default))
    )

    // Get default codecs.
    val videoCodec = sharedPref!!.getString(keyprefVideoCodec, getString(R.string.pref_videocodec_default))
    val audioCodec = sharedPref!!.getString(keyprefAudioCodec, getString(R.string.pref_audiocodec_default))

    // Check HW codec flag.
    val hwCodec = sharedPref!!.getBoolean(keyprefHwCodecAcceleration!!, java.lang.Boolean.valueOf(getString(R.string.pref_hwcodec_default)))

    // Get video resolution from settings.
    var videoWidth = 0
    var videoHeight = 0
    val resolution = sharedPref!!.getString(keyprefResolution, getString(R.string.pref_resolution_default))
    val dimensions = resolution.split("[ x]+")
    if (dimensions.size() == 2) {
      try {
        videoWidth = Integer.parseInt(dimensions[0])
        videoHeight = Integer.parseInt(dimensions[1])
      } catch (e: NumberFormatException) {
        videoWidth = 0
        videoHeight = 0
        Log.e(TAG, "Wrong video resolution setting: " + resolution)
      }

    }

    // Get camera fps from settings.
    var cameraFps = 0
    val fps = sharedPref!!.getString(keyprefFps, getString(R.string.pref_fps_default))
    val fpsValues = fps.split("[ x]+")
    if (fpsValues.size() == 2) {
      try {
        cameraFps = Integer.parseInt(fpsValues[0])
      } catch (e: NumberFormatException) {
        Log.e(TAG, "Wrong camera fps setting: " + fps)
      }

    }

    // Get video and audio start bitrate.
    var videoStartBitrate = 0
    var bitrateTypeDefault = getString(R.string.pref_startvideobitrate_default)
    var bitrateType = sharedPref!!.getString(keyprefVideoBitrateType, bitrateTypeDefault)
    if (bitrateType != bitrateTypeDefault) {
      val bitrateValue = sharedPref!!.getString(keyprefVideoBitrateValue, getString(R.string.pref_startvideobitratevalue_default))
      videoStartBitrate = Integer.parseInt(bitrateValue)
    }
    var audioStartBitrate = 0
    bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default)
    bitrateType = sharedPref!!.getString(keyprefAudioBitrateType, bitrateTypeDefault)
    if (bitrateType != bitrateTypeDefault) {
      val bitrateValue = sharedPref!!.getString(keyprefAudioBitrateValue, getString(R.string.pref_startaudiobitratevalue_default))
      audioStartBitrate = Integer.parseInt(bitrateValue)
    }

    // Test if CpuOveruseDetection should be disabled. By default is on.
    val cpuOveruseDetection = sharedPref!!.getBoolean(keyprefCpuUsageDetection!!,
      java.lang.Boolean.valueOf(getString(R.string.pref_cpu_usage_detection_default))
    )

    // Check statistics display option.
    val displayHud = sharedPref!!.getBoolean(keyprefDisplayHud!!, java.lang.Boolean.valueOf(getString(R.string.pref_displayhud_default)))

    // Start AppRTCDemo activity.
    Log.d(TAG, "Connecting to room " + roomId + " at URL " + roomUrl)
    if (validateUrl(roomUrl)) {
      val uri = Uri.parse(roomUrl)
      val intent = Intent(this, javaClass<CallActivity>())
      intent.setData(uri)
      intent.putExtra(CallActivity.EXTRA_ROOMID, roomId)
      intent.putExtra(CallActivity.EXTRA_LOOPBACK, loopback)
      intent.putExtra(CallActivity.EXTRA_VIDEO_CALL, videoCallEnabled)
      intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, videoWidth)
      intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, videoHeight)
      intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, cameraFps)
      intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, videoStartBitrate)
      intent.putExtra(CallActivity.EXTRA_VIDEOCODEC, videoCodec)
      intent.putExtra(CallActivity.EXTRA_HWCODEC_ENABLED, hwCodec)
      intent.putExtra(CallActivity.EXTRA_AUDIO_BITRATE, audioStartBitrate)
      intent.putExtra(CallActivity.EXTRA_AUDIOCODEC, audioCodec)
      intent.putExtra(CallActivity.EXTRA_CPUOVERUSE_DETECTION, cpuOveruseDetection)
      intent.putExtra(CallActivity.EXTRA_DISPLAY_HUD, displayHud)
      intent.putExtra(CallActivity.EXTRA_CMDLINE, commandLineRun)
      intent.putExtra(CallActivity.EXTRA_RUNTIME, runTimeMs)

      startActivityForResult(intent, CONNECTION_REQUEST)
    }
  }

  private fun validateUrl(url: String): Boolean {
    if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
      return true
    }

    AlertDialog.Builder(this).setTitle(getText(R.string.invalid_url_title)).setMessage(getString(R.string.invalid_url_text, url)).setCancelable(false).setNeutralButton(R.string.ok, object : DialogInterface.OnClickListener {
      override fun onClick(dialog: DialogInterface, id: Int) {
        dialog.cancel()
      }
    }).create().show()
    return false
  }

  private fun addRoomListener() = object : OnClickListener {
    override fun onClick(view: View) {
      val newRoom = roomEditText!!.getText().toString()
      if (newRoom.length() > 0 && !roomList!!.contains(newRoom)) {
        adapter!!.add(newRoom)
        adapter!!.notifyDataSetChanged()
      }
    }
  }

  private fun removeRoomListener() = object : OnClickListener {
    override fun onClick(view: View) {
      val selectedRoom = getSelectedItem()
      if (selectedRoom != null) {
        adapter!!.remove(selectedRoom)
        adapter!!.notifyDataSetChanged()
      }
    }
  }

  private fun getSelectedItem(): String? {
    var position = AdapterView.INVALID_POSITION
    if (roomListView!!.getCheckedItemCount() > 0 && adapter!!.getCount() > 0) {
      position = roomListView!!.getCheckedItemPosition()
      if (position >= adapter!!.getCount()) {
        position = AdapterView.INVALID_POSITION
      }
    }
    if (position != AdapterView.INVALID_POSITION) {
      return adapter!!.getItem(position)
    } else {
      return null
    }
  }

  companion object {
    private val TAG = "ConnectActivity"
    private val CONNECTION_REQUEST = 1
    private var commandLineRun = false
  }

}
