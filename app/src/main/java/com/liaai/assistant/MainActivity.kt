package com.liaai.assistant

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(12, 8, 25)
    private val purple = Color.rgb(177, 92, 255)
    private val prefs by lazy { getSharedPreferences("lia_secure_prefs", Context.MODE_PRIVATE) }
    private var apiKey = ""
    private var tts: TextToSpeech? = null
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var chat: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        apiKey = prefs.getString("gemini_key", "") ?: ""
        if (apiKey.isBlank()) showSetup() else showHome()
    }

    private fun page(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 20)
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        return root
    }

    private fun title(text: String, size: Float = 28f): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }

    private fun button(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(112, 45, 190))
            setOnClickListener { action() }
        }

    private fun showSetup() {
        val p = page()
        p.addView(title("LIA AI", 36f))
        p.addView(title("YOUR PERSONAL AI ASSISTANT", 13f))
        p.addView(title("Connect Gemini AI", 22f))
        val keyBox = EditText(this).apply {
            hint = "Paste Gemini API Key"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            inputType = 129
            setPadding(18, 14, 18, 14)
        }
        p.addView(keyBox, LinearLayout.LayoutParams(-1, -2))
        p.addView(button("CONNECT & START") {
            val key = keyBox.text.toString().trim()
            if (key.length < 20) {
                Toast.makeText(this, "Valid API key দিন", Toast.LENGTH_LONG).show()
            } else {
                apiKey = key
                prefs.edit().putString("gemini_key", key).apply()
                showHome()
            }
        })
        val note = TextView(this).apply {
            text = "API key গোপন রাখুন। এই প্রাথমিক সংস্করণে এটি অ্যাপের private preferences-এ রাখা হচ্ছে; production release-এর আগে Android Keystore encryption যোগ করতে হবে।"
            setTextColor(Color.LTGRAY); textSize = 12f; setPadding(4, 24, 4, 4)
        }
        p.addView(note)
    }

    private fun showHome() {
        val p = page()
        p.addView(title("LIA AI", 34f))
        p.addView(title("EVELYN • PERSONAL AI", 13f))
        status = TextView(this).apply {
            text = "Ready • Female voice setting"
            textSize = 15f; setTextColor(Color.rgb(205, 160, 255))
            gravity = Gravity.CENTER; setPadding(0, 12, 0, 18)
        }
        p.addView(status)
        p.addView(button("CHANGE GEMINI API KEY") { showSetup() })
        chat = TextView(this).apply {
            text = "Evelyn: হ্যালো! আমি LIA। কীভাবে সাহায্য করব?"
            textSize = 16f; setTextColor(Color.WHITE); setPadding(8, 22, 8, 22)
        }
        p.addView(chat)
        input = EditText(this).apply {
            hint = "এখানে লিখুন বা মাইক্রোফোন চাপুন"
            setHintTextColor(Color.LTGRAY); setTextColor(Color.WHITE)
        }
        p.addView(input)
        p.addView(button("SEND TO GEMINI") {
            val q = input.text.toString().trim()
            if (q.isNotEmpty()) askGemini(q)
        })
        p.addView(button("🎙 SPEAK") { listenOnce() })
        p.addView(button("TEST FEMALE VOICE") { speak("হ্যালো! আমি Evelyn। তোমার AI সহকারী।") })
        p.addView(button("RESET API KEY") {
            prefs.edit().remove("gemini_key").apply()
            apiKey = ""; showSetup()
        })
    }

    private fun askGemini(question: String) {
        chat.text = "You: $question\n\nEvelyn: ভাবছি..."
        status.text = "Connecting to Gemini..."
        thread {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 15000; readTimeout = 20000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val body = JSONObject().put("contents",
                    org.json.JSONArray().put(JSONObject().put("parts",
                        org.json.JSONArray().put(JSONObject().put("text", question)))))
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val raw = stream.bufferedReader().use { it.readText() }
                if (conn.responseCode !in 200..299) throw Exception("Gemini error ${conn.responseCode}: $raw")
                val answer = JSONObject(raw).getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                    .getJSONObject(0).getString("text")
                runOnUiThread {
                    chat.text = "You: $question\n\nEvelyn: $answer"
                    status.text = "Gemini connected"
                    speak(answer)
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Connection error"
                    chat.text = "You: $question\n\nEvelyn: ${e.message}"
                }
            }
        }
    }

    private fun listenOnce() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "এই ফোনে Speech Recognition পাওয়া যাচ্ছে না", Toast.LENGTH_LONG).show()
            return
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        status.text = "Listening once..."
        recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { runOnUiThread { status.text = "Speech error: $error" }; recognizer.destroy() }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                runOnUiThread { input.setText(spoken); if (spoken.isNotBlank()) askGemini(spoken) }
                recognizer.destroy()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-IN")
        }
        recognizer.startListening(intent)
    }

    private fun speak(text: String) {
        val engine = tts ?: return
        val voices = engine.voices ?: emptySet()
        val female = voices.firstOrNull { it.locale.language == "bn" && (it.name.contains("female", true) || it.name.contains("woman", true)) }
            ?: voices.firstOrNull { it.locale.language == "en" && (it.name.contains("female", true) || it.name.contains("woman", true)) }
        if (female != null) engine.voice = female
        engine.language = Locale("bn", "IN")
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lia-utterance")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts?.language = Locale("bn", "IN")
        }
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }
}
