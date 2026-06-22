package com.example.docscanner

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScannedDoc(
    val file: File,
    val name: String,
    val formattedSize: String,
    val formattedDate: String,
    val pageCount: Int,
    val fileType: String // "PDF" or "JPEG"
)

fun ScannedDoc.getDisplayName(): String {
    var cleanName = name
    val isSigned = cleanName.startsWith("Signed_")
    if (isSigned) {
        cleanName = cleanName.substring(7)
    }
    if (cleanName.startsWith("Imported_")) {
        if (cleanName.length > 25 && cleanName[24] == '_') {
            cleanName = cleanName.substring(25)
        } else {
            cleanName = cleanName.substring(9)
        }
    }
    if (isSigned) {
        val regex = Regex("_\\d{8}_\\d{6}$")
        val nameWithoutExt = cleanName.substringBeforeLast(".")
        val ext = cleanName.substringAfterLast(".", "")
        val match = regex.find(nameWithoutExt)
        if (match != null) {
            val stripped = nameWithoutExt.substring(0, match.range.first)
            cleanName = if (ext.isNotEmpty()) "$stripped.$ext" else stripped
        }
        return "Signed_$cleanName"
    }
    return cleanName
}


class DocumentManager(private val context: Context) {
    private val docDir = File(context.filesDir, "scanned_docs").apply {
        if (!exists()) mkdirs()
    }

    fun savePdf(uri: Uri): ScannedDoc? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = "Scan_$timeStamp.pdf"
        val destFile = File(docDir, name)
        return copyUriToFile(uri, destFile)?.let {
            val size = Formatter.formatShortFileSize(context, destFile.length())
            val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(destFile.lastModified()))
            ScannedDoc(
                file = destFile,
                name = name,
                formattedSize = size,
                formattedDate = dateStr,
                pageCount = 0,
                fileType = "PDF"
            )
        }
    }

    fun saveJpegs(uris: List<Uri>): List<ScannedDoc> {
        val result = mutableListOf<ScannedDoc>()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        uris.forEachIndexed { index, uri ->
            val name = "Scan_${timeStamp}_$index.jpg"
            val destFile = File(docDir, name)
            copyUriToFile(uri, destFile)?.let {
                val size = Formatter.formatShortFileSize(context, destFile.length())
                val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(destFile.lastModified()))
                result.add(
                    ScannedDoc(
                        file = destFile,
                        name = name,
                        formattedSize = size,
                        formattedDate = dateStr,
                        pageCount = 1,
                        fileType = "JPEG"
                    )
                )
            }
        }
        return result
    }

    fun getDocuments(): List<ScannedDoc> {
        val files = docDir.listFiles() ?: emptyArray()
        return files.filter { it.isFile && (it.name.endsWith(".pdf") || it.name.endsWith(".jpg")) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val size = Formatter.formatShortFileSize(context, file.length())
                val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                val fileType = if (file.name.endsWith(".pdf")) "PDF" else "JPEG"
                ScannedDoc(
                    file = file,
                    name = file.name,
                    formattedSize = size,
                    formattedDate = dateStr,
                    pageCount = if (fileType == "PDF") 0 else 1,
                    fileType = fileType
                )
            }
    }

    fun deleteDocument(doc: ScannedDoc): Boolean {
        return doc.file.delete()
    }

    fun getShareUri(doc: ScannedDoc): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", doc.file)
    }

    fun saveImageToGallery(doc: ScannedDoc): Uri? {
        val contentResolver = context.contentResolver
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val displayName = "Scan_$timeStamp.jpg"
        
        val imageDetails = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DocScanner")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = contentResolver.insert(collection, imageDetails)
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    java.io.FileInputStream(doc.file).use { inputStream ->
                        val buffer = ByteArray(4 * 1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    imageDetails.clear()
                    imageDetails.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, imageDetails, null, null)
                }
                return uri
            } catch (e: Exception) {
                e.printStackTrace()
                contentResolver.delete(uri, null, null)
            }
        }
        return null
    }

    fun importDocument(uri: Uri): ScannedDoc? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        val originalName = getFileName(uri) ?: "imported_file"
        val sanitizedOriginalName = originalName.replace("/", "_").replace("\\", "_")
        
        val isPdf = mimeType?.contains("pdf", ignoreCase = true) == true || 
                     sanitizedOriginalName.endsWith(".pdf", ignoreCase = true) == true
        
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = "Imported_${timeStamp}_${sanitizedOriginalName}"
        val destFile = File(docDir, name)
        
        return copyUriToFile(uri, destFile)?.let {
            val size = Formatter.formatShortFileSize(context, destFile.length())
            val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(destFile.lastModified()))
            val fileType = if (isPdf) "PDF" else "JPEG"
            ScannedDoc(
                file = destFile,
                name = name,
                formattedSize = size,
                formattedDate = dateStr,
                pageCount = if (isPdf) 0 else 1,
                fileType = fileType
            )
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun copyUriToFile(srcUri: Uri, destFile: File): File? {
        var inputStream: java.io.InputStream? = null
        var outputStream: java.io.FileOutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(srcUri) ?: return null
            outputStream = java.io.FileOutputStream(destFile)
            val buffer = ByteArray(4 * 1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            return destFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (ignored: Exception) {}
        }
    }
}
