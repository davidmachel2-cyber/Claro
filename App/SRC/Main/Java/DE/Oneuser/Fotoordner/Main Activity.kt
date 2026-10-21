package de.oneuser.fotoordner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.ViewGroup
import android.widget.*
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Read-only prototype: scanned content remains on device. No deletion or cloud sync. */
class MainActivity : Activity() {
    private val chooseFolder = 1001
    private val pool = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var scanButton: Button
    private val folderUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 20)
        }
        val title = TextView(this).apply { text = "FotoOrdner – Mein Speicher-Assistent"; textSize = 23f }
        root.addView(title)
        root.addView(TextView(this).apply {
            text = "Version 0.1 · Nur Analyse · Keine Dateien werden gelöscht oder hochgeladen."
            textSize = 15f; setPadding(0, 12, 0, 20)
        })
        val choose = Button(this).apply { text = "Ordner auswählen" }
        root.addView(choose)
        choose.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(intent, chooseFolder)
        }
        scanButton = Button(this).apply { text = "Speicher prüfen"; isEnabled = false }
        root.addView(scanButton)
        scanButton.setOnClickListener { scan() }
        status = TextView(this).apply { text = "Wähle zunächst einen Ordner aus, zum Beispiel Bilder oder Downloads."; setPadding(0, 14, 0, 14) }
        root.addView(status)
        root.addView(TextView(this).apply { text = "Doppelte Dateien – Vorschläge zur eigenen Auswahl"; textSize = 18f })
        val scroll = ScrollView(this)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(results)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(TextView(this).apply { text = "KI-Vorschläge und Papierkorb: für eine spätere Version vorgesehen. Diese Version löscht nichts." })
        setContentView(root)
    }

    @Deprecated("Activity result API supported for this minimal no-dependency prototype")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != chooseFolder || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { }
        if (!folderUris.contains(uri)) folderUris.add(uri)
        scanButton.isEnabled = true
        status.text = "${folderUris.size} Ordner ausgewählt. Tippe auf „Speicher prüfen“."
    }

    private data class Entry(val name: String, val uri: Uri, val size: Long)

    private fun scan() {
        scanButton.isEnabled = false
        results.removeAllViews()
        status.text = "Dateien werden lokal geprüft …"
        val roots = folderUris.toList()
        pool.execute {
            try {
                val entries = ArrayList<Entry>()
                val seenUris = HashSet<String>()
                for (tree in roots) {
                    val rootId = DocumentsContract.getTreeDocumentId(tree)
                    collect(tree, rootId, entries, seenUris)
                }
                // Pre-group by size: avoids hashing unique-size files unnecessarily.
                val bySize = entries.filter { it.size > 0 }.groupBy { it.size }.values.filter { it.size > 1 }
                val groups = mutableListOf<List<Entry>>()
                for (batch in bySize) {
                    for (group in batch.groupBy { sha256(it.uri) }.values) {
                        if (group.size > 1) groups.add(group)
                    }
                }
                val bytes = groups.sumOf { group -> group.first().size * (group.size - 1) }
                runOnUiThread {
                    status.text = "${entries.size} Dateien geprüft · ${groups.size} Gruppen identischer Dateien · theoretisch ${pretty(bytes)} mehrfach belegt."
                    if (groups.isEmpty()) addLine("Keine exakten Duplikate in den gewählten Ordnern gefunden.")
                    groups.sortedByDescending { it.first().size * (it.size - 1) }.take(100).forEachIndexed { idx, g ->
                        addLine("${idx + 1}. ${g.size} identische Dateien (${pretty(g.first().size)} je Datei):\n" + g.joinToString("\n") { "• ${it.name}" })
                    }
                    if (groups.size > 100) addLine("Weitere ${groups.size - 100} Gruppen nicht dargestellt.")
                    scanButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Scan nicht vollständig: ${e.localizedMessage ?: "Zugriff nicht möglich"}"
                    scanButton.isEnabled = true
                }
            }
        }
    }

    private fun collect(tree: Uri, documentId: String, output: MutableList<Entry>, seen: MutableSet<String>) {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
        if (!seen.add(doc.toString())) return
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
        contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val typeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idCol)
                val name = cursor.getString(nameCol) ?: "Unbenannt"
                val type = cursor.getString(typeCol)
                val size = if (cursor.isNull(sizeCol)) -1L else cursor.getLong(sizeCol)
                if (type == DocumentsContract.Document.MIME_TYPE_DIR) {
                    collect(tree, id, output, seen)
                } else if (size >= 0) {
                    output.add(Entry(name, DocumentsContract.buildDocumentUriUsingTree(tree, id), size))
                }
            }
        }
    }

    private fun sha256(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        contentResolver.openInputStream(uri)?.use { stream ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(buf)
                if (count < 0) break
                if (count > 0) digest.update(buf, 0, count)
            }
        } ?: throw IllegalStateException("Datei kann nicht gelesen werden: $uri")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun addLine(line: String) {
        results.addView(TextView(this).apply { text = line; textSize = 15f; setPadding(0, 16, 0, 16) })
    }

    private fun pretty(size: Long): String = when {
        size >= 1_000_000_000 -> "%.2f GB".format(size / 1_000_000_000.0)
        size >= 1_000_000 -> "%.1f MB".format(size / 1_000_000.0)
        else -> "$size Byte"
    }

    override fun onDestroy() {
        pool.shutdownNow()
        super.onDestroy()
    }
}
