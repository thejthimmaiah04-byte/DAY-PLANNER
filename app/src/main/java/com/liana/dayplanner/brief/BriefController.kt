package com.liana.dayplanner.brief

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.planner.DayPlanner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** One selectable Android TTS voice, already filtered to English. */
data class VoiceOption(
    val id: String,              // Voice.name — stable identifier
    val country: String,         // "IN", "US", "GB", …
    val accentLabel: String,     // "India", "United States", …
    val displayName: String,     // human label for the row
    val quality: Int,            // Voice.getQuality()
    val needsNetwork: Boolean
)

class BriefController(private val context: Context) {

    var visible by mutableStateOf(false); private set
    var section by mutableStateOf(""); private set
    var text by mutableStateOf(""); private set

    var includeWeather by mutableStateOf(prefs.getBoolean("weather", true))
    var includeNews by mutableStateOf(prefs.getBoolean("news", true))
    var rate by mutableStateOf(prefs.getFloat("rate", 0.95f))

    // Voice selection (persisted). accent = ISO country; voiceId = specific Voice.name
    var accent by mutableStateOf(prefs.getString("accent", "US") ?: "US")
        private set
    var voiceId by mutableStateOf(prefs.getString("voiceId", "") ?: "")
        private set
    var voicesReady by mutableStateOf(false); private set
    val voices = mutableStateListOf<VoiceOption>()

    /** true = overlay shown only to configure voice, no brief playing */
    var settingsOnly by mutableStateOf(false); private set

    private val prefs get() = context.getSharedPreferences("arc_brief", Context.MODE_PRIVATE)

    private var tts: TextToSpeech? = null
    @Volatile private var skip = false
    @Volatile private var stop = false
    private var speechDone: CompletableDeferred<Unit>? = null

    fun setWeatherPref(on: Boolean) { includeWeather = on; prefs.edit().putBoolean("weather", on).apply() }
    fun setNewsPref(on: Boolean) { includeNews = on; prefs.edit().putBoolean("news", on).apply() }
    fun setRatePref(r: Float) { rate = r; prefs.edit().putFloat("rate", r).apply() }

    fun skipSection() { skip = true; tts?.stop() }
    fun end() { stop = true; tts?.stop(); visible = false; settingsOnly = false }

    fun start(scope: CoroutineScope, tasks: List<Task>) {
        if (visible) return
        settingsOnly = false; visible = true; stop = false
        scope.launch { run(tasks) }
    }

    /** Show the overlay purely to pick voice/accent, without speaking the brief. */
    fun openSettings(scope: CoroutineScope) {
        if (visible) return
        settingsOnly = true; visible = true
        loadVoices(scope)
    }

    /** The list of accents (ISO country codes) that actually have a voice installed. */
    val availableAccents: List<Pair<String, String>>
        get() = voices.map { it.country to it.accentLabel }.distinct()

    private fun accentLabelFor(country: String): String = when (country) {
        "IN" -> "India"; "US" -> "United States"; "GB" -> "United Kingdom"
        "AU" -> "Australia"; "CA" -> "Canada"; "IE" -> "Ireland"
        "ZA" -> "South Africa"; "NG" -> "Nigeria"; "SG" -> "Singapore"
        "NZ" -> "New Zealand"; "PH" -> "Philippines"
        else -> Locale("en", country).getDisplayCountry(Locale.ENGLISH).ifBlank { country }
    }

    /** Load/refresh the list of installed English voices. Safe to call from UI. */
    fun loadVoices(scope: CoroutineScope) {
        scope.launch {
            if (!ensureTts()) return@launch
            val engine = tts ?: return@launch
            val all = runCatching { engine.voices }.getOrNull().orEmpty()
                .filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                .ifEmpty {
                    runCatching { engine.voices }.getOrNull().orEmpty()
                        .filter { it.locale.language == "en" }
                }
            val opts = all.map { v ->
                val country = v.locale.country.ifBlank { "US" }
                VoiceOption(
                    id = v.name,
                    country = country,
                    accentLabel = accentLabelFor(country),
                    displayName = niceName(v),
                    quality = v.quality,
                    needsNetwork = v.isNetworkConnectionRequired
                )
            }.sortedWith(compareByDescending<VoiceOption> { it.quality }.thenBy { it.displayName })
            voices.clear(); voices.addAll(opts)

            // Default accent = United States if present, else first available
            if (availableAccents.none { it.first == accent }) {
                accent = availableAccents.firstOrNull { it.first == "US" }?.first
                    ?: availableAccents.firstOrNull()?.first ?: "US"
            }
            if (voices.none { it.id == voiceId }) {
                voiceId = defaultVoiceFor(accent)?.id ?: ""
            }
            applyVoice()
            voicesReady = true
        }
    }

    private fun niceName(v: Voice): String {
        // Google voices look like "en-in-x-ene-network"; make them human
        val q = when {
            v.quality >= Voice.QUALITY_VERY_HIGH -> "Very natural"
            v.quality >= Voice.QUALITY_HIGH -> "Natural"
            v.quality >= Voice.QUALITY_NORMAL -> "Standard"
            else -> "Basic"
        }
        val variant = v.name.substringAfterLast("-x-", "").ifBlank { v.name }
            .replace("-network", "").replace("-local", "").replace("-", " ")
            .replaceFirstChar { it.uppercase() }
        return "$q · $variant"
    }

    fun bestVoiceFor(country: String): VoiceOption? =
        voices.filter { it.country == country }.maxByOrNull { it.quality }

    // Google TTS variant codes that are commonly male (best-effort — the API
    // exposes no gender field). Used only to pick a sensible default voice.
    private val maleVariantCodes = setOf(
        "iob", "iog", "iom", "tpc", "tpd", "iad", "eey", "ifp")

    private fun defaultVoiceFor(country: String): VoiceOption? {
        val pool = voices.filter { it.country == country }
        val male = pool.filter { v ->
            maleVariantCodes.any { v.id.contains("-x-$it", true) || v.id.contains(it, true) }
        }.maxByOrNull { it.quality }
        return male ?: pool.maxByOrNull { it.quality }
    }

    fun setAccent(scope: CoroutineScope, country: String) {
        accent = country
        prefs.edit().putString("accent", country).apply()
        // pick a sensible default voice in that accent (male-leaning)
        defaultVoiceFor(country)?.let { setVoice(scope, it.id, preview = true) }
    }

    fun setVoice(scope: CoroutineScope, id: String, preview: Boolean = false) {
        voiceId = id
        prefs.edit().putString("voiceId", id).apply()
        applyVoice()
        if (preview) scope.launch { previewVoice() }
    }

    private fun applyVoice() {
        val engine = tts ?: return
        val chosen = engine.voices?.firstOrNull { it.name == voiceId }
        if (chosen != null) {
            engine.voice = chosen
        } else {
            val loc = Locale("en", accent)
            if (engine.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE)
                engine.language = loc else engine.language = Locale.ENGLISH
        }
    }

    private suspend fun previewVoice() {
        if (!ensureTts()) return
        val engine = tts ?: return
        engine.setSpeechRate(rate)
        applyVoice()
        engine.stop()
        engine.speak("This is how I will sound during your morning brief.",
            TextToSpeech.QUEUE_FLUSH, null, "arc-preview")
    }

    fun previewCurrent(scope: CoroutineScope) { scope.launch { previewVoice() } }

    private suspend fun ensureTts(): Boolean {
        if (tts != null) return true
        val ready = CompletableDeferred<Boolean>()
        val engine = TextToSpeech(context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }
        if (!ready.await()) return false
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { if (id == "arc-last") speechDone?.complete(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { speechDone?.complete(Unit) }
            override fun onStop(id: String?, interrupted: Boolean) { speechDone?.complete(Unit) }
        })
        tts = engine
        applyVoice()
        return true
    }

    private suspend fun initTts(): Boolean {
        val ok = ensureTts()
        if (ok) applyVoice()
        return ok
    }

    private suspend fun speak(body: String) {
        val engine = tts ?: return
        engine.setSpeechRate(rate)
        val sentences = body.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        if (sentences.isEmpty()) return
        speechDone = CompletableDeferred()
        sentences.forEachIndexed { i, s ->
            val id = if (i == sentences.lastIndex) "arc-last" else "arc-$i"
            engine.speak(s, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
        speechDone?.await()
    }

    /** Set by the app so the brief can speak the user's vision statement. */
    var visionStatement: String = ""

    private suspend fun run(tasks: List<Task>) {
        val hasTts = initTts()
        val weatherJob = if (includeWeather)
            CoroutineScope(Dispatchers.IO).async { weatherWithCache() } else null
        val newsJob = if (includeNews)
            CoroutineScope(Dispatchers.IO).async { newsWithCache() } else null

        val day = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH))
        val hour = LocalTime.now().hour
        val opening = (if (hour < 12) "Good morning." else if (hour < 17) "Good afternoon." else "Good evening.") +
            " Today is $day. Here is your brief."

        data class Section(val name: String, val get: suspend () -> String?)
        val sections = listOf(
            Section("Welcome") { opening },
            Section("Weather") { weatherJob?.await() ?: if (includeWeather) "Weather is unavailable right now, moving on." else null },
            Section("Headlines") { newsJob?.await() ?: if (includeNews) "The headlines are unavailable right now, moving on." else null },
            Section("Your day") { scheduleScript(tasks) },
            Section("Momentum") { momentumScript(tasks) },
            Section("Go well") { "That is everything. Move with intention — one block at a time. Have a great day." }
        )

        for (sec in sections) {
            if (stop) break
            skip = false
            section = sec.name
            text = "…"
            val body = sec.get() ?: continue
            if (stop) break
            if (skip) continue
            text = body
            if (hasTts) speak(body)
            else kotlinx.coroutines.delay((2000 + body.length * 40L).coerceAtMost(12000))
        }
        visible = false
    }

    /* ---------------- Content ---------------- */

    private fun fetch(url: String, timeoutMs: Int = 12000, redirects: Int = 4): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.instanceFollowRedirects = true
        // Look like a real browser — several news CDNs reject unknown agents
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0 Mobile Safari/537.36")
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
        conn.setRequestProperty("Accept-Encoding", "gzip")
        val code = conn.responseCode
        // Manually follow cross-protocol redirects (http<->https) which the JDK won't
        if (code in 300..399) {
            val loc = conn.getHeaderField("Location")
            conn.disconnect()
            if (loc != null && redirects > 0) return fetch(loc, timeoutMs, redirects - 1)
            throw Exception("redirect with no location")
        }
        if (code !in 200..299) { conn.disconnect(); throw Exception("HTTP $code") }
        val raw = conn.inputStream
        val stream = if (conn.contentEncoding?.contains("gzip", true) == true)
            java.util.zip.GZIPInputStream(raw) else raw
        return stream.use { it.readBytes().decodeToString() }
    }

    private val wmo = mapOf(
        0 to "clear skies", 1 to "mostly clear skies", 2 to "partly cloudy skies", 3 to "overcast skies",
        45 to "fog", 48 to "freezing fog", 51 to "light drizzle", 53 to "drizzle", 55 to "heavy drizzle",
        61 to "light rain", 63 to "rain", 65 to "heavy rain", 71 to "light snow", 73 to "snow",
        75 to "heavy snow", 80 to "passing showers", 81 to "showers", 82 to "heavy showers",
        95 to "a thunderstorm", 96 to "a thunderstorm with hail", 99 to "a severe thunderstorm")

    private data class GeoFix(val lat: Double, val lon: Double, val city: String?)

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Real device position via GPS/network. Null if no permission or no fix available. */
    private fun deviceGeoFix(): GeoFix? {
        if (!hasLocationPermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // Best recent cached fix across all enabled providers.
        var best: Location? = null
        for (p in try { lm.getProviders(true) } catch (_: Exception) { emptyList<String>() }) {
            val l = try { lm.getLastKnownLocation(p) } catch (_: SecurityException) { null } ?: continue
            if (best == null || l.time > best!!.time) best = l
        }
        // If nothing cached or it's over 30 min stale, ask for one fresh fix.
        val stale = best == null || System.currentTimeMillis() - best!!.time > 30 * 60 * 1000
        if (stale) requestFreshFix(lm)?.let { best = it }

        val loc = best ?: return null
        return GeoFix(loc.latitude, loc.longitude, reverseCity(loc.latitude, loc.longitude))
    }

    /** Block for a single up-to-date location, up to 8s. Best-effort. */
    private fun requestFreshFix(lm: LocationManager): Location? {
        if (!hasLocationPermission()) return null
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        val latch = CountDownLatch(1)
        val holder = arrayOfNulls<Location>(1)
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val signal = android.os.CancellationSignal()
                lm.getCurrentLocation(provider, signal, context.mainExecutor) { l ->
                    holder[0] = l; latch.countDown()
                }
                if (!latch.await(8, TimeUnit.SECONDS)) signal.cancel()
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(l: Location) { holder[0] = l; latch.countDown() }
                    override fun onProviderDisabled(p: String) {}
                    override fun onProviderEnabled(p: String) {}
                    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                }
                @Suppress("DEPRECATION")
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                latch.await(8, TimeUnit.SECONDS)
                lm.removeUpdates(listener)
            }
        } catch (_: SecurityException) { return null } catch (_: Exception) { return null }
        return holder[0]
    }

    /** Turn coordinates into a city name for the "In <city>" phrase. */
    private fun reverseCity(lat: Double, lon: Double): String? = try {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale.ENGLISH).getFromLocation(lat, lon, 1)
            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    } catch (_: Exception) { null }

    private suspend fun weatherScript(): String = withContext(Dispatchers.IO) {
        // Prefer a real device fix; fall back to coarse IP geolocation.
        val fix = deviceGeoFix() ?: run {
            val loc = JSONObject(fetch("https://ipwho.is/"))
            if (!loc.optBoolean("success", true)) throw Exception()
            GeoFix(loc.getDouble("latitude"), loc.getDouble("longitude"),
                loc.optString("city").ifBlank { null })
        }
        val lat = fix.lat; val lon = fix.lon
        val city = fix.city ?: ""
        val d = JSONObject(fetch(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto"))
        val cur = d.getJSONObject("current")
        val daily = d.getJSONObject("daily")
        val t = cur.getDouble("temperature_2m").toInt()
        val feels = cur.getDouble("apparent_temperature").toInt()
        val hi = daily.getJSONArray("temperature_2m_max").getDouble(0).toInt()
        val lo = daily.getJSONArray("temperature_2m_min").getDouble(0).toInt()
        val rain = daily.optJSONArray("precipitation_probability_max")?.optInt(0, -1) ?: -1
        val sky = wmo[cur.getInt("weather_code")] ?: "mixed conditions"
        val sb = StringBuilder()
        sb.append(if (city.isNotBlank()) "In $city, it" else "It")
        sb.append(" is currently $t degrees with $sky.")
        if (kotlin.math.abs(feels - t) >= 3) sb.append(" It feels more like $feels.")
        sb.append(" Expect a high of $hi and a low of $lo today.")
        val art = if (Regex("^(8|11$|18)").containsMatchIn(rain.toString())) "an" else "a"
        if (rain >= 50) sb.append(" There is $art $rain percent chance of rain, so an umbrella might be wise.")
        else if (rain >= 25) sb.append(" ${art.replaceFirstChar { it.uppercase() }} $rain percent chance of rain — probably fine, but keep an eye on the sky.")
        sb.toString()
    }

    private val upscRegex = Regex(
        "supreme court|high court|parliament|lok sabha|rajya sabha|\\bbill\\b|\\bact\\b|policy|scheme|yojana|ministry|cabinet|\\brbi\\b|gdp|inflation|budget|fiscal|election commission|\\bsebi\\b|niti aayog|isro|drdo|summit|treaty|environment|climate|wildlife|forest|tribunal|constitution|amendment|governor|judiciary|reservation|census|g20|\\bun\\b|\\bwho\\b|commission|federal|panchayat|agreement|mission",
        RegexOption.IGNORE_CASE)

    // Accepts both <item><title>…</title> (RSS) and <entry><title>…</title> (Atom)
    private fun feedTitles(xml: String): List<String> =
        Regex("<(?:item|entry)[ >](.*?)</(?:item|entry)>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).mapNotNull { m ->
                Regex("<title[^>]*>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</title>",
                    RegexOption.DOT_MATCHES_ALL)
                    .find(m.groupValues[1])?.groupValues?.get(1)
                    ?.replace(Regex("<[^>]+>"), "")?.trim()
                    ?.replace("&amp;", "&")?.replace("&quot;", "\"")
                    ?.replace("&#039;", "'")?.replace("&apos;", "'")?.replace("&#8217;", "'")
            }.filter { it.isNotBlank() }.toList()

    private val worldFeeds = listOf(
        "https://feeds.bbci.co.uk/news/world/rss.xml",
        "https://moxie.foxnews.com/google-publisher/world.xml",
        "https://www.aljazeera.com/xml/rss/all.xml"
    )
    private val indiaFeeds = listOf(
        "https://www.thehindu.com/news/national/feeder/default.rss",
        "https://feeds.bbci.co.uk/news/world/asia/india/rss.xml",
        "https://indianexpress.com/section/india/feed/",
        "https://www.livemint.com/rss/news"
    )

    private fun firstWorkingFeed(feeds: List<String>): List<String> {
        for (f in feeds) {
            val titles = runCatching { feedTitles(fetch(f)) }.getOrDefault(emptyList())
            if (titles.isNotEmpty()) return titles
        }
        return emptyList()
    }

    private suspend fun newsScript(): String = withContext(Dispatchers.IO) {
        val world = firstWorkingFeed(worldFeeds)
        val india = firstWorkingFeed(indiaFeeds)
        val parts = mutableListOf<String>()
        if (world.isNotEmpty()) parts.add("A quick look at the world. " +
            world.take(3).mapIndexed { i, t -> "${i + 1}. $t." }.joinToString(" "))
        if (india.isNotEmpty()) {
            val ranked = india.filter { upscRegex.containsMatchIn(it) } +
                         india.filterNot { upscRegex.containsMatchIn(it) }
            // Cache the top UPSC-relevant headlines for the Current Affairs screen
            saveHeadlines(ranked.take(12))
            parts.add("Now, from India — worth noting for your UPSC preparation. " +
                ranked.take(4).mapIndexed { i, t -> "${i + 1}. $t." }.joinToString(" "))
        }
        if (parts.isEmpty()) throw Exception()
        parts.joinToString(" ")
    }

    private fun saveHeadlines(list: List<String>) {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString("ca_headlines", arr.toString())
            .putString("ca_date", LocalDate.now().toString()).apply()
    }

    /** UPSC-relevant headlines captured on the last brief, for the Current Affairs screen. */
    fun capturedHeadlines(): Pair<String, List<String>> {
        val date = prefs.getString("ca_date", "") ?: ""
        val raw = prefs.getString("ca_headlines", "[]") ?: "[]"
        val arr = runCatching { org.json.JSONArray(raw) }.getOrNull() ?: org.json.JSONArray()
        return date to (0 until arr.length()).map { arr.getString(it) }
    }

    private fun scheduleScript(tasks: List<Task>): String {
        val plan = DayPlanner.plan(tasks, LocalDate.now(),
            settings = com.liana.dayplanner.data.SettingsStore.current(context))
        if (plan.blocks.isEmpty() && plan.overflow.isEmpty())
            return "Your schedule is clear today. A good moment to invest in your two year vision — or simply rest."
        val mins = plan.blocks.sumOf { it.endMin - it.startMin }
        val hours = if (mins >= 60) "${mins / 60}${if (mins % 60 >= 30) " and a half" else ""} hours"
                    else "$mins minutes"
        fun sayTime(m: Int): String {
            // Spoken 24-hour, e.g. "09:00" -> "09 hundred hours", "17:30" -> "17 30 hours"
            val h = m / 60; val mm = m % 60
            val hh = "%02d".format(h)
            return if (mm == 0) "$hh hundred hours" else "$hh %02d hours".format(mm)
        }
        val sb = StringBuilder()
        val uniq = plan.blocks.distinctBy { it.task.id }
        sb.append("Now, your day. You have ${uniq.size} ")
        sb.append(if (uniq.size == 1) "task" else "tasks")
        sb.append(" planned, about $hours of focused effort. ")
        for (b in plan.blocks) {
            sb.append("At ${sayTime(b.startMin)}, ${b.task.title}")
            if (b.part != null) sb.append(", part ${b.part.replace("/", " of ")}")
            sb.append(" — ${b.task.priority.label.lowercase()} priority. ")
        }
        if (plan.triage.isNotEmpty()) {
            val word = if (plan.triage.size == 1) "task" else "tasks"
            sb.append("You also have ${plan.triage.size} slipped $word waiting for a decision on the Today screen. ")
        }
        if (plan.overflow.isNotEmpty())
            sb.append("When you find a spare moment: ${plan.overflow.joinToString("; ") { it.title }}.")
        return sb.toString()
    }

    /** Streak + an occasional reminder of the two-year vision, for motivation. */
    private fun momentumScript(tasks: List<Task>): String? {
        val today = LocalDate.now()
        val doneDays = tasks.filter { it.isDone && it.completedAt != null }
            .map { java.time.Instant.ofEpochMilli(it.completedAt!!)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() }.toSet()
        var streak = 0
        var cur = today
        if (cur !in doneDays) cur = cur.minusDays(1)
        while (cur in doneDays) { streak++; cur = cur.minusDays(1) }

        val sb = StringBuilder()
        if (streak >= 2) sb.append("You are on a $streak day streak. Keep it alive. ")
        // Speak the vision roughly every third day to keep it fresh, not naggy
        if (visionStatement.isNotBlank() && today.toEpochDay() % 3 == 0L)
            sb.append("Remember why you are doing this. $visionStatement")
        return sb.toString().ifBlank { null }
    }

    /* ---------------- Offline cache for weather & news ---------------- */

    private fun saveCache(key: String, value: String) {
        prefs.edit().putString("cache_$key", value)
            .putString("cache_${key}_date", LocalDate.now().toString()).apply()
    }

    private fun loadCache(key: String): Pair<String, String>? {
        val v = prefs.getString("cache_$key", null) ?: return null
        val d = prefs.getString("cache_${key}_date", "") ?: ""
        return v to d
    }

    private suspend fun weatherWithCache(): String? {
        val fresh = runCatching { weatherScript() }.getOrNull()
        if (fresh != null) { saveCache("weather", fresh); return fresh }
        val cached = loadCache("weather") ?: return null
        return if (cached.second == LocalDate.now().toString()) cached.first
               else "From your last update: ${cached.first}"
    }

    private suspend fun newsWithCache(): String? {
        val fresh = runCatching { newsScript() }.getOrNull()
        if (fresh != null) { saveCache("news", fresh); return fresh }
        val cached = loadCache("news") ?: return null
        return "You are offline, so here are the most recent headlines I have. ${cached.first}"
    }
}
