package com.rfm.ms

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.*
import java.net.*
import java.util.Collections

class Peer(val num: String, var name: String, var ip: String, var seen: Long)
class Msg(val out: Boolean, val type: Int, val time: Long, val path: String, val text: String)

object Core {
    const val TCP = 8777
    const val UDP = 8778
    private lateinit var app: Context
    lateinit var myNum: String
    lateinit var myName: String
    lateinit var media: File
    val peers = java.util.concurrent.ConcurrentHashMap<String, Peer>()
    val contacts = LinkedHashMap<String, String>()
    val unread = HashMap<String, Int>()
    val extra = java.util.concurrent.CopyOnWriteArrayList<String>()
    var cur: String? = null
    @Volatile var uiOpen = false
    @Volatile var running = false
    var onMsg: ((String, Msg) -> Unit)? = null
    var onNotify: ((String, Msg) -> Unit)? = null
    private val ui = Handler(Looper.getMainLooper())
    private var ss: ServerSocket? = null
    private var ds: DatagramSocket? = null

    fun init(c: Context) {
        if (::app.isInitialized) return
        app = c.applicationContext
        val sp = app.getSharedPreferences("ms", 0)
        myNum = sp.getString("num", null) ?: ("7" + (10000000..99999999).random()).also { sp.edit().putString("num", it).apply() }
        myName = sp.getString("name", null) ?: (android.os.Build.MODEL ?: "MS").also { sp.edit().putString("name", it).apply() }
        media = File(app.getExternalFilesDir(null) ?: app.filesDir, "media"); media.mkdirs()
        loadContacts()
    }

    fun esc(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")
    fun unesc(s: String): String {
        val sb = StringBuilder(); var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { val n = s[i + 1]; sb.append(if (n == 'n') '\n' else if (n == 't') '\t' else n); i += 2 } else { sb.append(c); i++ }
        }
        return sb.toString()
    }
    fun safe(s: String) = s.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    fun nameOf(k: String) = contacts[k] ?: peers[k]?.name ?: k
    fun online(k: String) = peers[k]?.let { System.currentTimeMillis() - it.seen < 6000 } == true

    private fun chatFile(n: String) = File(app.filesDir, "chat_$n.txt")
    fun append(n: String, m: Msg) {
        chatFile(n).appendText("${if (m.out) 1 else 0}\t${m.type}\t${m.time}\t${esc(m.path)}\t${esc(m.text)}\n")
    }
    fun load(n: String): List<Msg> {
        val f = chatFile(n); if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull {
            val a = it.split("\t")
            if (a.size < 5) null else Msg(a[0] == "1", a[1].toInt(), a[2].toLong(), unesc(a[3]), unesc(a[4]))
        }
    }
    fun saveContacts() { File(app.filesDir, "contacts.txt").writeText(contacts.entries.joinToString("\n") { "${it.key}\t${esc(it.value)}" }) }
    private fun loadContacts() {
        val f = File(app.filesDir, "contacts.txt")
        if (f.exists()) f.readLines().forEach { val a = it.split("\t"); if (a.size >= 2) contacts[a[0]] = unesc(a[1]) }
    }

    private fun ifaces(): List<NetworkInterface> = try { Collections.list(NetworkInterface.getNetworkInterfaces()) } catch (e: Exception) { emptyList() }
    private fun bcasts(): List<InetAddress> {
        val l = ArrayList<InetAddress>()
        for (n in ifaces()) for (a in n.interfaceAddresses) a.broadcast?.let { l.add(it) }
        try { l.add(InetAddress.getByName("255.255.255.255")) } catch (e: Exception) {}
        return l
    }

    fun start() {
        if (running) return
        running = true
        Thread {
            try { val s = ServerSocket(TCP); ss = s; while (running) { val c = s.accept(); Thread { handle(c) }.start() } } catch (e: Exception) {}
        }.start()
        Thread {
            try {
                val d = DatagramSocket(); d.broadcast = true
                while (running) {
                    val m = "MS|$myNum|$myName".toByteArray()
                    val targets = ArrayList<InetAddress>(bcasts())
                    for (pr in peers.values) try { targets.add(InetAddress.getByName(pr.ip)) } catch (e: Exception) {}
                    for (x in extra) try { targets.add(InetAddress.getByName(x)) } catch (e: Exception) {}
                    for (a in targets) try { d.send(DatagramPacket(m, m.size, a, UDP)) } catch (e: Exception) {}
                    Thread.sleep(1500)
                }
                d.close()
            } catch (e: Exception) {}
        }.start()
        Thread {
            try {
                val d = DatagramSocket(null); d.reuseAddress = true; d.bind(InetSocketAddress(UDP)); ds = d
                val b = ByteArray(512)
                while (running) {
                    val p = DatagramPacket(b, b.size); d.receive(p)
                    val a = String(p.data, 0, p.length).split("|")
                    if (a.size >= 3 && a[0] == "MS" && a[1] != myNum) {
                        val ip = p.address.hostAddress ?: continue
                        val nm = a.drop(2).joinToString("|")
                        val pr = peers[a[1]]
                        if (pr == null) peers[a[1]] = Peer(a[1], nm, ip, System.currentTimeMillis())
                        else { pr.ip = ip; pr.seen = System.currentTimeMillis(); pr.name = nm }
                    }
                }
            } catch (e: Exception) {}
        }.start()
    }

    fun stop() {
        running = false
        try { ss?.close() } catch (e: Exception) {}
        try { ds?.close() } catch (e: Exception) {}
    }

    // returns an error message, or null on success
    fun transmit(num: String, type: Int, label: String, f: File?): String? {
        val p = peers[num]
        if (p == null || System.currentTimeMillis() - p.seen > 15000) return "المستخدم غير متصل حاليًا"
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(p.ip, TCP), 4000)
                val o = DataOutputStream(BufferedOutputStream(s.getOutputStream(), 65536))
                o.writeUTF(myNum); o.writeUTF(myName); o.writeByte(type); o.writeUTF(label)
                o.writeLong(f?.length() ?: 0L)
                if (f != null) FileInputStream(f).use { it.copyTo(o) }
                o.flush()
            }
            null
        } catch (e: Exception) { "تعذر الإرسال" }
    }

    private fun handle(s: Socket) {
        try {
            val i = DataInputStream(BufferedInputStream(s.getInputStream(), 65536))
            val num = i.readUTF(); val nm = i.readUTF(); val t = i.readByte().toInt(); val label = i.readUTF(); var left = i.readLong()
            var path = ""
            if (t != 0) {
                val f = File(media, "${System.currentTimeMillis()}_" + safe(label))
                FileOutputStream(f).use { o ->
                    val b = ByteArray(65536)
                    while (left > 0) {
                        val r = i.read(b, 0, minOf(b.size.toLong(), left).toInt())
                        if (r < 0) throw EOFException()
                        o.write(b, 0, r); left -= r
                    }
                }
                path = f.absolutePath
            }
            val m = Msg(false, t, System.currentTimeMillis(), path, label)
            ui.post { deliver(num, nm, m) }
        } catch (e: Exception) {
        } finally { try { s.close() } catch (e: Exception) {} }
    }

    private fun deliver(num: String, nm: String, m: Msg) {
        if (!contacts.containsKey(num)) { contacts[num] = nm; saveContacts() }
        append(num, m)
        if (!(uiOpen && cur == num)) {
            unread[num] = (unread[num] ?: 0) + 1
            if (!uiOpen) onNotify?.invoke(num, m)
        }
        onMsg?.invoke(num, m)
    }
}
