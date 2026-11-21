package com.rfm.ms

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.graphics.PathParser
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : Activity() {
    private val WA = "https://wa.me/201027814323"
    private val W = 0xFFFFFFFF.toInt()
    private val MUTED = 0xFF9AA4C7.toInt()
    private val GREEN = 0xFF3BE07A.toInt()
    private val C1 = 0xFF7C5CFF.toInt()
    private val C2 = 0xFF00D4FF.toInt()
    private val BG = 0xFF0B0F1A.toInt()

    private val SEND = "M2.01,21L23,12 2.01,3 2,10l15,2 -15,2z"
    private val MIC = "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z"
    private val CLIP = "M16.5,6v11.5c0,2.21 -1.79,4 -4,4s-4,-1.79 -4,-4V5c0,-1.38 1.12,-2.5 2.5,-2.5s2.5,1.12 2.5,2.5v10.5c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V6H10v9.5c0,1.38 1.12,2.5 2.5,2.5s2.5,-1.12 2.5,-2.5V5c0,-2.21 -1.79,-4 -4,-4S7,2.79 7,5v12.5c0,3.04 2.46,5.5 5.5,5.5s5.5,-2.46 5.5,-5.5V6h-1.5z"
    private val BACK = "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"
    private val ADD = "M15,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM6,10V7H4v3H1v2h3v3h2v-3h3v-2H6zM15,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"
    private val PLAY = "M8,5v14l11,-7z"
    private val PAUSE = "M6,19h4V5H6v14zM14,5v14h4V5h-4z"
    private val COPY = "M16,1H4c-1.1,0 -2,0.9 -2,2v14h2V3h12V1zM19,5H8c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2zM19,21H8V7h11v14z"
    private val FILE = "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6zM13,9V3.5L18.5,9H13z"
    private val DL = "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z"
    private val WIFI = "M1,9l2,2c4.97,-4.97 13.03,-4.97 18,0l2,-2C16.93,2.93 7.08,2.93 1,9zM9,17l3,3 3,-3c-1.65,-1.66 -4.34,-1.66 -6,0zM5,13l2,2c2.76,-2.76 7.24,-2.76 10,0l2,-2C15.14,9.14 8.87,9.14 5,13z"

    private lateinit var root: FrameLayout
    private val peers: java.util.concurrent.ConcurrentHashMap<String, Peer> get() = Core.peers
    private val contacts: LinkedHashMap<String, String> get() = Core.contacts
    private val unread: HashMap<String, Int> get() = Core.unread
    private val media: File get() = Core.media
    private val myNum: String get() = Core.myNum
    private val myName: String get() = Core.myName
    private var cur: String?
        get() = Core.cur
        set(v) { Core.cur = v }
    private var msgBox: LinearLayout? = null
    private var scroll: ScrollView? = null
    private var listBox: LinearLayout? = null
    private var chatStatus: TextView? = null
    private val h = Handler(Looper.getMainLooper())
    @Volatile private var alive = true
    private var player: MediaPlayer? = null
    private var playingPath: String? = null
    private var playingIcon: Ic? = null
    private var rec: MediaRecorder? = null
    private var recFile: File? = null
    private var recStart = 0L
    private var pickType = 1
    private var after: (() -> Unit)? = null
    private var wm: WifiP2pManager? = null
    private var wch: WifiP2pManager.Channel? = null
    private var p2pBox: LinearLayout? = null
    private var p2pStatus: TextView? = null
    private var p2pReg = false

    private val p2pRx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    try { wm?.requestPeers(wch) { l -> p2pShowPeers(l.deviceList.toList()) } } catch (e: SecurityException) {}
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    try { wm?.requestConnectionInfo(wch) { onP2pInfo(it) } } catch (e: SecurityException) {}
                }
            }
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Core.init(applicationContext)
        cur = null
        root = FrameLayout(this)
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(BG, 0xFF171C33.toInt()))
        window.statusBarColor = BG
        window.navigationBarColor = 0xFF171C33.toInt()
        setContentView(root)
        try {
            val si = Intent(this, MsService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(si) else startService(si)
        } catch (e: Exception) {}
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 8)
        Core.onMsg = { num, m ->
            if (cur == num) { msgBox?.addView(bubble(m)); toBottom() } else { if (cur == null) renderList() }
        }
        val fl = FrameLayout(this)
        fl.addView(Splash(this))
        fl.addView(badge(), FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(28) })
        screen(fl)
        h.postDelayed(object : Runnable {
            override fun run() { if (cur == null) renderList() else updStatus(); if (alive) h.postDelayed(this, 2500) }
        }, 2500)
    }

    override fun onResume() {
        super.onResume(); Core.uiOpen = true
        cur?.let { Core.unread.remove(it) }
    }
    override fun onPause() { Core.uiOpen = false; super.onPause() }
    override fun onDestroy() {
        alive = false; Core.uiOpen = false; Core.onMsg = null
        try { player?.release() } catch (e: Exception) {}
        try { rec?.release() } catch (e: Exception) {}
        if (p2pReg) try { unregisterReceiver(p2pRx) } catch (e: Exception) {}
        super.onDestroy()
    }
    override fun onBackPressed() { if (cur != null) showMain() else super.onBackPressed() }

    private fun afterSplash() { showMain(); intent?.getStringExtra("open")?.let { openChat(it) } }

    // ---------- helpers ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private fun lp(w: Int = -1, hh: Int = -2) = LinearLayout.LayoutParams(w, hh)
    private fun screen(v: View) { root.removeAllViews(); root.addView(v) }
    private fun tv(t: String, sz: Float, c: Int = W, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = sz; setTextColor(c); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun card(c: Int = 0x1AFFFFFF, r: Int = 22) = GradientDrawable().apply { cornerRadius = dp(r).toFloat(); setColor(c); setStroke(dp(1), 0x22FFFFFF) }
    private fun rr(c: Int, r: Int) = GradientDrawable().apply { cornerRadius = dp(r).toFloat(); setColor(c) }
    private fun gradR(r: Int) = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(C1, C2)).apply { cornerRadius = dp(r).toFloat() }
    private fun circ() = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(C1, C2)).apply { shape = GradientDrawable.OVAL }
    private fun fmt(n: String) = n.chunked(3).joinToString(" ")
    private fun fsz(b: Long) = if (b < 1024) "$b B" else if (b < 1048576) "${b / 1024} KB" else "${b / 1048576} MB"
    private fun nameOf(k: String) = Core.nameOf(k)
    private fun online(k: String) = Core.online(k)
    private fun safe(s: String) = Core.safe(s)
    private fun append(n: String, m: Msg) = Core.append(n, m)
    private fun load(n: String) = Core.load(n)
    private fun saveContacts() = Core.saveContacts()

    inner class Ic(c: Context, private var d: String, private val tint: Int = W, private var mirror: Boolean = false) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
        private var path: Path = PathParser.createPathFromPathData(d)
        fun set(nd: String, m: Boolean) { d = nd; mirror = m; path = PathParser.createPathFromPathData(nd); invalidate() }
        override fun onDraw(cv: Canvas) {
            cv.save()
            if (mirror) { cv.translate(width.toFloat(), 0f); cv.scale(-width / 24f, height / 24f) } else cv.scale(width / 24f, height / 24f)
            cv.drawPath(path, p); cv.restore()
        }
    }
    private fun iconBtn(d: String, mirror: Boolean = false, bg: Drawable? = null, a: () -> Unit) = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        if (bg != null) background = bg
        addView(Ic(context, d, W, mirror), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        setOnClickListener { a() }
    }

    private fun drawLogo(c: Canvas, cx: Float, cy: Float, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = RectF(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2)
        p.shader = LinearGradient(r.left, r.top, r.right, r.bottom, C1, C2, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, s * 0.28f, s * 0.28f, p)
        p.shader = null; p.color = W
        val b = RectF(cx - s * 0.3f, cy - s * 0.26f, cx + s * 0.3f, cy + s * 0.14f)
        val path = Path(); path.addRoundRect(b, s * 0.12f, s * 0.12f, Path.Direction.CW)
        path.moveTo(cx - s * 0.16f, cy + s * 0.12f); path.lineTo(cx - s * 0.22f, cy + s * 0.3f); path.lineTo(cx + s * 0.02f, cy + s * 0.12f); path.close()
        c.drawPath(path, p)
        p.color = C1; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); p.textSize = s * 0.26f
        c.drawText("MS", cx, b.centerY() - (p.ascent() + p.descent()) / 2, p)
    }
    inner class Logo(c: Context) : View(c) {
        override fun onDraw(cv: Canvas) { drawLogo(cv, width / 2f, height / 2f, min(width, height) * 0.92f) }
    }

    private fun badge(): View {
        val t = tv("</>  rFM", 14f, W, true)
        t.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        t.setPadding(dp(18), dp(8), dp(18), dp(8)); t.background = card(0x14FFFFFF, 20)
        t.post {
            val w = t.paint.measureText(t.text.toString())
            val sh = LinearGradient(0f, 0f, w, 0f, intArrayOf(C1, C2, W, C1), null, Shader.TileMode.MIRROR)
            t.paint.shader = sh
            ValueAnimator.ofFloat(0f, w * 2).apply {
                duration = 2200; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
                addUpdateListener { val m = Matrix(); m.setTranslate(it.animatedValue as Float, 0f); sh.setLocalMatrix(m); t.invalidate() }
            }.start()
        }
        t.setOnClickListener { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WA))) } catch (e: Exception) { toast("+201027814323") } }
        return t
    }

    // ---------- splash ----------
    inner class Splash(c: Context) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val t0 = System.currentTimeMillis()
        private val rnd = Random(7)
        private val px = FloatArray(30) { rnd.nextFloat() }
        private val py = FloatArray(30) { rnd.nextFloat() }
        private val ps = FloatArray(30) { 0.4f + rnd.nextFloat() }
        private var done = false
        private fun orb(cv: Canvas, x: Float, y: Float, r: Float, col: Int) {
            p.shader = RadialGradient(x, y, r, intArrayOf((col and 0x00FFFFFF) or 0x55000000, col and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            cv.drawCircle(x, y, r, p); p.shader = null
        }
        override fun onDraw(cv: Canvas) {
            val t = (System.currentTimeMillis() - t0) / 1000f
            val w = width.toFloat(); val hh = height.toFloat()
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, 0f, w, hh, BG, 0xFF1B1440.toInt(), Shader.TileMode.CLAMP)
            cv.drawRect(0f, 0f, w, hh, p); p.shader = null
            orb(cv, w * (0.2f + 0.1f * sin(t)), hh * 0.25f, w * 0.7f, C1)
            orb(cv, w * (0.85f - 0.1f * cos(t)), hh * 0.72f, w * 0.8f, C2)
            for (i in 0 until 30) {
                val y = hh - ((t * ps[i] * 0.12f + py[i]) % 1f) * hh
                p.color = Color.argb((50 + 90 * abs(sin(t + i))).toInt(), 255, 255, 255)
                cv.drawCircle(px[i] * w + 14f * sin(t + i), y, dp(1) + ps[i] * dp(2), p)
            }
            val cx = w / 2; val cy = hh * 0.38f
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2).toFloat()
            for (i in 0..2) {
                val f = (t * 0.5f + i / 3f) % 1f
                p.color = Color.argb(((1 - f) * 120).toInt(), 0, 212, 255)
                cv.drawCircle(cx, cy, dp(70) + f * dp(120), p)
            }
            p.style = Paint.Style.FILL
            val k = min(1f, t / 0.9f); val s = k - 1f
            val sc = 1f + 2.70158f * s * s * s + 1.70158f * s * s
            drawLogo(cv, cx, cy, dp(120) * sc)
            val a1 = min(1f, maxOf(0f, (t - 0.6f) / 0.6f))
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            p.textSize = dp(40).toFloat()
            p.shader = LinearGradient(cx - dp(60), 0f, cx + dp(60), 0f, intArrayOf(C1, C2), null, Shader.TileMode.CLAMP)
            p.alpha = (a1 * 255).toInt()
            cv.drawText("MS", cx, cy + dp(125), p); p.shader = null
            val a2 = min(1f, maxOf(0f, (t - 1.1f) / 0.6f))
            p.color = MUTED; p.alpha = (a2 * 255).toInt(); p.textSize = dp(15).toFloat()
            p.typeface = Typeface.DEFAULT
            cv.drawText("تواصل بلا حدود … بلا إنترنت", cx, cy + dp(158), p)
            val bw = w * 0.4f; val by = hh - dp(110).toFloat()
            p.color = 0x22FFFFFF
            cv.drawRoundRect(cx - bw / 2, by, cx + bw / 2, by + dp(4), dp(2).toFloat(), dp(2).toFloat(), p)
            p.shader = LinearGradient(cx - bw / 2, 0f, cx + bw / 2, 0f, intArrayOf(C1, C2), null, Shader.TileMode.CLAMP)
            cv.drawRoundRect(cx - bw / 2, by, cx - bw / 2 + bw * min(1f, t / 3.2f), by + dp(4), dp(2).toFloat(), dp(2).toFloat(), p)
            p.shader = null
            if (t > 3.4f && !done) { done = true; post { afterSplash() } }
            if (!done) postInvalidateOnAnimation()
        }
    }

    // ---------- main screen ----------
    private fun showMain() {
        cur = null; chatStatus = null; msgBox = null; scroll = null; p2pBox = null; p2pStatus = null
        try { player?.release() } catch (e: Exception) {}
        player = null; playingPath = null; playingIcon = null
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(44), dp(20), dp(16)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(Logo(this), LinearLayout.LayoutParams(dp(44), dp(44)))
        top.addView(tv("MS", 24f, W, true).apply { setPadding(dp(10), 0, dp(10), 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(iconBtn(WIFI, false, card(0x1AFFFFFF, 22)) { askP(p2pPerm()) { showP2p() } })
        top.addView(View(this), LinearLayout.LayoutParams(dp(8), 1))
        top.addView(iconBtn(ADD, false, card(0x1AFFFFFF, 22)) { addContact() })
        c.addView(top)
        val nc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(16))
            background = gradR(22); layoutParams = lp().apply { topMargin = dp(18) }
        }
        val nr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nr.addView(tv(fmt(myNum), 30f, W, true).apply { letterSpacing = 0.06f }, LinearLayout.LayoutParams(0, -2, 1f))
        nr.addView(Ic(this, COPY, W, false), LinearLayout.LayoutParams(dp(22), dp(22)))
        nc.addView(tv("رقمك الافتراضي — اضغط للنسخ", 12f, 0xCCFFFFFF.toInt()))
        nc.addView(nr)
        nc.setOnClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("MS", myNum))
            toast("تم نسخ رقمك")
        }
        c.addView(nc)
        c.addView(tv("المحادثات", 15f, MUTED, true).apply { setPadding(0, dp(20), 0, dp(4)) })
        val sv = ScrollView(this); val lb = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sv.addView(lb); listBox = lb
        c.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        c.addView(badge(), lp(-2).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8) })
        screen(c); renderList()
    }

    private fun renderList() {
        val lb = listBox ?: return
        lb.removeAllViews()
        val keys = LinkedHashSet<String>(); keys.addAll(contacts.keys); keys.addAll(peers.keys)
        val sorted = keys.sortedByDescending { if (online(it)) 1 else 0 }
        if (sorted.isEmpty()) lb.addView(tv("لا توجد محادثات بعد.\nافتح التطبيق على جهاز قريب (نفس الواي فاي أو الهوت سبوت) أو اضغط أيقونة الواي فاي للاتصال المباشر، أو أضف رقمًا بزر +", 14f, MUTED).apply { gravity = Gravity.CENTER; setPadding(0, dp(40), 0, 0) })
        for (k in sorted) lb.addView(row(k))
    }

    private fun row(k: String): View {
        val on = online(k)
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12)); background = card(); layoutParams = lp().apply { topMargin = dp(10) }
        }
        r.addView(tv(nameOf(k).take(1).uppercase(), 18f, W, true).apply { gravity = Gravity.CENTER; background = circ() }, LinearLayout.LayoutParams(dp(46), dp(46)))
        val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, dp(12), 0) }
        mid.addView(tv(nameOf(k), 16f, W, true))
        mid.addView(tv(fmt(k) + (if (on) "  •  متصل" else ""), 12f, if (on) GREEN else MUTED))
        r.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        val u = unread[k] ?: 0
        if (u > 0) r.addView(tv("$u", 12f, W, true).apply { gravity = Gravity.CENTER; background = circ() }, LinearLayout.LayoutParams(dp(24), dp(24)))
        r.setOnClickListener { openChat(k) }
        return r
    }

    private fun addContact() {
        val et = EditText(this).apply { hint = "اكتب رقم المستخدم"; inputType = InputType.TYPE_CLASS_NUMBER; setPadding(dp(20), dp(14), dp(20), dp(14)) }
        AlertDialog.Builder(this).setTitle("إضافة جهة اتصال").setView(et).setPositiveButton("إضافة") { _, _ ->
            val n = et.text.toString().filter { it.isDigit() }
            if (n.length >= 6 && n != myNum) {
                if (!contacts.containsKey(n)) { contacts[n] = peers[n]?.name ?: "مستخدم $n"; saveContacts() }
                renderList()
            } else toast("رقم غير صحيح")
        }.setNegativeButton("إلغاء", null).show()
    }

    // ---------- Wi-Fi Direct ----------
    private fun p2pPerm() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else Manifest.permission.ACCESS_FINE_LOCATION
    private fun askP(perm: String, go: () -> Unit) {
        if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) go()
        else { after = go; requestPermissions(arrayOf(perm), 7) }
    }

    private fun showP2p() {
        cur = null; listBox = null
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(44), dp(24), dp(16)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(iconBtn(BACK, true) { p2pLeave(false); showMain() })
        top.addView(tv("اتصال مباشر (Wi-Fi Direct)", 20f, W, true).apply { setPadding(dp(8), 0, 0, 0) })
        c.addView(top)
        p2pStatus = tv("بدون راوتر وبدون إنترنت. فعّل الواي فاي والموقع على الجهازين، افتح هذه الصفحة على الجهازين، ثم اختر الجهاز الآخر.", 13f, MUTED).apply { setPadding(0, dp(12), 0, dp(8)) }
        c.addView(p2pStatus)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(tv("🔍  بحث", 16f, W, true).apply {
            gravity = Gravity.CENTER; background = gradR(22); setPadding(dp(16), dp(12), dp(16), dp(12)); setOnClickListener { p2pSearch() }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        row.addView(tv("⛔  قطع الاتصال", 16f, W, true).apply {
            gravity = Gravity.CENTER; background = card(); setPadding(dp(16), dp(12), dp(16), dp(12)); setOnClickListener { p2pLeave(true) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        c.addView(row)
        val sv = ScrollView(this); val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sv.addView(box); p2pBox = box
        c.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        c.addView(badge(), lp(-2).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8) })
        screen(c); p2pSearch()
    }

    private fun p2pInit() {
        if (wm != null) return
        val m = getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        if (m == null) { toast("الجهاز لا يدعم Wi-Fi Direct"); return }
        wm = m; wch = m.initialize(this, mainLooper, null)
        registerReceiver(p2pRx, IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION); addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        })
        p2pReg = true
        try { m.requestConnectionInfo(wch) { onP2pInfo(it) } } catch (e: SecurityException) {}
    }

    private fun p2pSearch() {
        p2pInit(); val m = wm ?: return
        try {
            m.discoverPeers(wch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { p2pStatus?.text = "جارٍ البحث عن أجهزة قريبة… افتح نفس الصفحة على الجهاز الآخر" }
                override fun onFailure(r: Int) { p2pStatus?.text = "تعذر البحث (كود $r) — فعّل الواي فاي والموقع ثم اضغط بحث" }
            })
        } catch (e: SecurityException) { toast("الصلاحية مطلوبة") }
    }

    private fun p2pShowPeers(l: List<WifiP2pDevice>) {
        val b = p2pBox ?: return
        b.removeAllViews()
        for (d in l) b.addView(tv("📱  ${d.deviceName}", 16f, W, true).apply {
            setPadding(dp(18), dp(14), dp(18), dp(14)); background = card(); layoutParams = lp().apply { topMargin = dp(10) }
            setOnClickListener { p2pConnect(d) }
        })
    }

    private fun p2pConnect(d: WifiP2pDevice) {
        val cfg = WifiP2pConfig().apply { deviceAddress = d.deviceAddress }
        try {
            wm?.connect(wch, cfg, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { p2pStatus?.text = "جارٍ الاتصال بـ ${d.deviceName}… وافق على الطلب في الجهاز الآخر" }
                override fun onFailure(r: Int) { p2pStatus?.text = "فشل الاتصال (كود $r)" }
            })
        } catch (e: SecurityException) { toast("الصلاحية مطلوبة") }
    }

    private fun onP2pInfo(info: WifiP2pInfo?) {
        if (info == null) return
        Core.extra.clear()
        if (info.groupFormed) {
            if (!info.isGroupOwner) info.groupOwnerAddress?.hostAddress?.let { Core.extra.add(it) }
            p2pStatus?.text = if (info.isGroupOwner) "✅ متصل — جهازك هو مالك المجموعة. ارجع للمحادثات." else "✅ متصل بالمجموعة. ارجع للمحادثات."
        } else p2pStatus?.text = "غير متصل"
    }

    private fun p2pLeave(remove: Boolean) {
        try {
            if (remove) { wm?.removeGroup(wch, null); Core.extra.clear(); toast("تم قطع الاتصال") }
            else wm?.stopPeerDiscovery(wch, null)
        } catch (e: SecurityException) {}
    }

    // ---------- chat screen ----------
    private fun openChat(num: String) {
        cur = num; unread.remove(num)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(num.hashCode())
        if (!contacts.containsKey(num)) { contacts[num] = nameOf(num); saveContacts() }
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(40), dp(12), dp(10)); background = rr(0x14FFFFFF, 0)
        }
        bar.addView(iconBtn(BACK, true) { showMain() })
        bar.addView(tv(nameOf(num).take(1).uppercase(), 16f, W, true).apply { gravity = Gravity.CENTER; background = circ() }, LinearLayout.LayoutParams(dp(40), dp(40)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        info.addView(tv(nameOf(num), 16f, W, true))
        chatStatus = tv("", 12f, MUTED); info.addView(chatStatus)
        bar.addView(info)
        c.addView(bar)
        val sv = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8)) }
        sv.addView(box); scroll = sv; msgBox = box
        c.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        load(num).forEach { box.addView(bubble(it)) }
        toBottom()
        val inp = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(14)); background = rr(0x14FFFFFF, 0)
        }
        inp.addView(iconBtn(CLIP) { attachMenu() })
        val et = EditText(this).apply {
            hint = "اكتب رسالة…"; setTextColor(W); setHintTextColor(0xFF6B7499.toInt())
            background = card(0x1AFFFFFF, 24); setPadding(dp(16), dp(10), dp(16), dp(10)); maxLines = 4
        }
        inp.addView(et, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(8), 0) })
        val ico = Ic(this, MIC, W, false)
        val act = FrameLayout(this).apply { background = circ(); addView(ico, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)) }
        inp.addView(act, LinearLayout.LayoutParams(dp(48), dp(48)))
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { if (s.isNullOrBlank()) ico.set(MIC, false) else ico.set(SEND, true) }
        })
        act.setOnTouchListener { _, e ->
            if (et.text.isNotBlank()) {
                if (e.action == MotionEvent.ACTION_UP) { val t = et.text.toString().trim(); et.setText(""); sendText(num, t) }
            } else when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRec(et)
                    else { after = { toast("تم السماح، اضغط مطولًا على الميكروفون للتسجيل") }; requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7) }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopRec(num, et)
            }
            true
        }
        c.addView(inp)
        screen(c); updStatus()
    }

    private fun updStatus() {
        val n = cur ?: return
        val on = online(n)
        chatStatus?.text = fmt(n) + (if (on) "  •  متصل الآن" else "  •  غير متصل")
        chatStatus?.setTextColor(if (on) GREEN else MUTED)
    }
    private fun toBottom() { scroll?.post { scroll?.fullScroll(View.FOCUS_DOWN) } }

    private fun bubble(m: Msg): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = lp(-2).apply { topMargin = dp(6); gravity = if (m.out) Gravity.START else Gravity.END }
        }
        val b = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(9), dp(12), dp(7))
            background = if (m.out) gradR(18) else rr(0x26FFFFFF, 18)
        }
        val maxW = (resources.displayMetrics.widthPixels * 0.72f).toInt()
        when (m.type) {
            0 -> b.addView(tv(m.text, 15f).apply { maxWidth = maxW })
            1 -> {
                val bm = decode(m.path, 700)
                if (bm != null) {
                    val iv = ImageView(this); iv.setImageBitmap(bm); iv.adjustViewBounds = true; iv.scaleType = ImageView.ScaleType.FIT_CENTER
                    b.addView(iv, LinearLayout.LayoutParams(dp(220), -2))
                    iv.setOnClickListener { showImage(m.path) }
                } else b.addView(tv("🖼 ${m.text}", 14f))
            }
            2 -> {
                val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                val pi = Ic(this, PLAY, W, false)
                val pb = FrameLayout(this).apply { background = rr(0x33FFFFFF, 20); addView(pi, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)) }
                r.addView(pb, LinearLayout.LayoutParams(dp(40), dp(40)))
                val wf = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), 0, dp(10), 0) }
                val hs = intArrayOf(8, 16, 24, 12, 20, 28, 14, 22, 10, 18, 26, 12, 20, 8, 16, 24, 14, 10)
                for (x in hs) wf.addView(View(this).apply { background = rr(0xCCFFFFFF.toInt(), 2) }, LinearLayout.LayoutParams(dp(3), dp(x)).apply { setMargins(dp(1), 0, dp(1), 0) })
                r.addView(wf)
                b.addView(r)
                r.setOnClickListener { play(m.path, pi) }
            }
            else -> {
                val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                r.addView(Ic(this, FILE, W, false), LinearLayout.LayoutParams(dp(30), dp(30)))
                val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, dp(10), 0) }
                col.addView(tv(m.text, 14f, W, true).apply { maxWidth = dp(170) })
                col.addView(tv(fsz(File(m.path).length()), 11f, 0xAAFFFFFF.toInt()))
                r.addView(col)
                r.addView(Ic(this, DL, W, false), LinearLayout.LayoutParams(dp(22), dp(22)))
                b.addView(r)
                r.setOnClickListener { save(m.path, m.text) }
            }
        }
        b.addView(tv(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.time)), 10f, 0xAAFFFFFF.toInt()).apply { gravity = Gravity.END }, lp())
        wrap.addView(b)
        return wrap
    }

    private fun decode(path: String, max: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var s = 1; while (o.outWidth / s > max) s *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
    }
    private fun showImage(path: String) {
        val d = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val iv = ImageView(this); iv.setImageBitmap(decode(path, 2000)); iv.setOnClickListener { d.dismiss() }
        d.setContentView(iv); d.show()
    }

    private fun play(path: String, icon: Ic) {
        try { player?.stop() } catch (e: Exception) {}
        try { player?.release() } catch (e: Exception) {}
        player = null
        playingIcon?.set(PLAY, false)
        if (playingPath == path) { playingPath = null; playingIcon = null; return }
        try {
            val mp = MediaPlayer()
            mp.setDataSource(path); mp.prepare(); mp.start()
            mp.setOnCompletionListener { icon.set(PLAY, false); playingPath = null }
            player = mp; icon.set(PAUSE, false); playingIcon = icon; playingPath = path
        } catch (e: Exception) { playingPath = null; toast("تعذر تشغيل الرسالة الصوتية") }
    }

    // ---------- recording / sending ----------
    private fun startRec(et: EditText) {
        try {
            val f = File(media, "voice_${System.currentTimeMillis()}.m4a")
            val r = MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(f.absolutePath); r.prepare(); r.start()
            rec = r; recFile = f; recStart = System.currentTimeMillis()
            et.hint = "🔴 جارٍ التسجيل… اترك الزر للإرسال"
        } catch (e: Exception) { rec = null; toast("تعذر التسجيل") }
    }
    private fun stopRec(num: String, et: EditText) {
        val r = rec ?: return
        rec = null; et.hint = "اكتب رسالة…"
        var ok = System.currentTimeMillis() - recStart > 800
        try { r.stop() } catch (e: Exception) { ok = false }
        r.release()
        val f = recFile ?: return
        if (ok) sendMedia(num, 2, "voice.m4a", f) else f.delete()
    }
    private fun attachMenu() {
        AlertDialog.Builder(this).setItems(arrayOf("🖼  صورة", "📁  ملف")) { _, i ->
            pickType = if (i == 0) 1 else 3
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = if (i == 0) "image/*" else "*/*"
                addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }, 9)
        }.show()
    }
    override fun onActivityResult(rq: Int, rs: Int, d: Intent?) {
        super.onActivityResult(rq, rs, d)
        val num = cur ?: return
        if (rq != 9 || rs != RESULT_OK || d == null) return
        val uris = ArrayList<Uri>(); val cd = d.clipData
        if (cd != null) { for (i in 0 until cd.itemCount) uris.add(cd.getItemAt(i).uri) } else { d.data?.let { uris.add(it) } }
        val type = pickType
        for (u in uris) Thread {
            try {
                val nm = fname(u); val f = File(media, "${System.currentTimeMillis()}_" + safe(nm))
                contentResolver.openInputStream(u)!!.use { i -> FileOutputStream(f).use { o -> i.copyTo(o) } }
                runOnUiThread { sendMedia(num, type, nm, f) }
            } catch (e: Exception) { runOnUiThread { toast("تعذر قراءة الملف") } }
        }.start()
    }
    private fun fname(u: Uri): String {
        contentResolver.query(u, null, null, null, null)?.use {
            if (it.moveToFirst()) { val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) return it.getString(i) }
        }
        return "file_${System.currentTimeMillis()}"
    }
    private fun sendText(num: String, t: String) {
        val m = Msg(true, 0, System.currentTimeMillis(), "", t)
        append(num, m); msgBox?.addView(bubble(m)); toBottom()
        Thread { Core.transmit(num, 0, t, null)?.let { e -> runOnUiThread { toast(e) } } }.start()
    }
    private fun sendMedia(num: String, type: Int, label: String, f: File) {
        val m = Msg(true, type, System.currentTimeMillis(), f.absolutePath, label)
        append(num, m); msgBox?.addView(bubble(m)); toBottom()
        Thread { Core.transmit(num, type, label, f)?.let { e -> runOnUiThread { toast(e) } } }.start()
    }

    // ---------- save to Downloads ----------
    private fun save(path: String, label: String) {
        if (Build.VERSION.SDK_INT >= 29) saveCopy(path, label)
        else if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) saveCopy(path, label)
        else { after = { saveCopy(path, label) }; requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 7) }
    }
    private fun saveCopy(path: String, label: String) {
        Thread {
            try {
                val nm = safe(label)
                val out: OutputStream = if (Build.VERSION.SDK_INT >= 29) {
                    val v = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, nm); put(MediaStore.Downloads.RELATIVE_PATH, "Download/MS") }
                    contentResolver.openOutputStream(contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)!!)!!
                } else {
                    val d = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MS"); d.mkdirs()
                    FileOutputStream(File(d, nm))
                }
                out.use { o -> FileInputStream(File(path)).use { it.copyTo(o) } }
                runOnUiThread { toast("تم الحفظ في Download/MS") }
            } catch (e: Exception) { runOnUiThread { toast("تعذر الحفظ") } }
        }.start()
    }
    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        if (rc == 7 && r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) after?.invoke()
    }
}
