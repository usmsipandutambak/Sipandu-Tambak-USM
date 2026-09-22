package id.ac.usm.sipandutambak.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object CsvExportHelper {

    fun exportAndShareCsv(
        context: Context,
        fileName: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        if (rows.isEmpty()) {
            Toast.makeText(context, "Tidak ada data sensor untuk diekspor!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val file = File(exportDir, "$fileName.csv")
            val writer = FileWriter(file)

            // 1. Tulis Header CSV
            writer.append(headers.joinToString(",")).append("\n")

            // 2. Tulis Baris Data
            for (row in rows) {
                val escapedRow = row.map { "\"${it.replace("\"", "\"\"")}\"" }
                writer.append(escapedRow.joinToString(",")).append("\n")
            }
            writer.flush()
            writer.close()

            // 3. Ambil URI aman via FileProvider
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 4. Buka Dialog Share Sistem Android (WhatsApp, Drive, Sheets, Email, dll)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Export Data Sensor - $fileName")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Simpan / Kirim File CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuat file CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}