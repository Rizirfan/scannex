package com.example.docscanner.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.docscanner.DocumentManager
import com.example.docscanner.ScannedDoc
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignScreen(
  filePath: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val documentManager = remember { DocumentManager(context.applicationContext) }
  val file = remember { File(filePath) }
  
  var documentBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var inkColor by remember { mutableStateOf(Color.Black) }
  
  val paths = remember { mutableStateListOf<Path>() }
  var currentPath by remember { mutableStateOf<Path?>(null) }
  
  var canvasWidth by remember { mutableStateOf(0f) }
  var canvasHeight by remember { mutableStateOf(0f) }

  // Load Background Document page
  LaunchedEffect(file) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      val loaded = if (file.name.endsWith(".pdf")) {
        renderPdfPageToBitmap(file)
      } else {
        BitmapFactory.decodeFile(file.absolutePath)
      }
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        documentBitmap = loaded
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Sign Document", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          TextButton(
            onClick = {
              if (paths.isEmpty()) {
                Toast.makeText(context, "Please draw a signature first", Toast.LENGTH_SHORT).show()
                return@TextButton
              }
              val bitmap = documentBitmap
              if (bitmap != null && canvasWidth > 0 && canvasHeight > 0) {
                val scaleX = bitmap.width.toFloat() / canvasWidth
                val scaleY = bitmap.height.toFloat() / canvasHeight
                
                val androidPaths = paths.map { it.asAndroidPath() }
                
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                  val signedFile = saveSignedDocument(
                    context = context,
                    originalFile = file,
                    paths = androidPaths,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    inkColor = inkColor
                  )
                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (signedFile != null) {
                      Toast.makeText(context, "Saved signed document successfully", Toast.LENGTH_SHORT).show()
                      onBack()
                    } else {
                      Toast.makeText(context, "Failed to save signed document", Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              }
            },
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
          ) {
            Icon(Icons.Default.Done, contentDescription = "Save", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
      // Background Image Canvas in the middle
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(16.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color.White)
          .onGloballyPositioned { coordinates ->
            canvasWidth = coordinates.size.width.toFloat()
            canvasHeight = coordinates.size.height.toFloat()
          },
        contentAlignment = Alignment.Center
      ) {
        // Document page background
        documentBitmap?.let { bmp ->
          androidx.compose.foundation.Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Document background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
          )
        } ?: Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }

        // Draw Canvas overlay
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
              detectDragGestures(
                onDragStart = { offset ->
                  val path = Path().apply { moveTo(offset.x, offset.y) }
                  currentPath = path
                  paths.add(path)
                },
                onDrag = { change, _ ->
                  change.consume()
                  currentPath?.lineTo(change.position.x, change.position.y)
                  // Trigger updates in Compose state tracking
                  val lastIndex = paths.lastIndex
                  if (lastIndex >= 0) {
                    paths[lastIndex] = Path().apply { addPath(currentPath!!) }
                  }
                },
                onDragEnd = {
                  currentPath = null
                }
              )
            }
        ) {
          paths.forEach { path ->
            drawPath(
              path = path,
              color = inkColor,
              style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
          }
        }

        // Separate Floating Clear Button
        if (paths.isNotEmpty()) {
          OutlinedButton(
            onClick = { paths.clear() },
            modifier = Modifier
              .padding(8.dp)
              .height(28.dp)
              .align(Alignment.TopEnd),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color.White.copy(alpha = 0.9f),
              contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(horizontal = 8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = "Clear signature",
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Drawing toolbar at bottom
      Surface(
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text("Ink Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
          Spacer(modifier = Modifier.width(12.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ColorDot(color = Color.Black, selected = inkColor == Color.Black) { inkColor = Color.Black }
            ColorDot(color = Color.Blue, selected = inkColor == Color.Blue) { inkColor = Color.Blue }
            ColorDot(color = Color.Red, selected = inkColor == Color.Red) { inkColor = Color.Red }
            ColorDot(color = Color.Green, selected = inkColor == Color.Green) { inkColor = Color.Green }
          }
        }
      }
    }
  }
}

@Composable
fun ColorDot(
  color: Color,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(20.dp)
      .clip(CircleShape)
      .background(color)
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    if (selected) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(if (color == Color.White) Color.Black else Color.White)
      )
    }
  }
}

fun renderPdfPageToBitmap(file: File, pageIndex: Int = 0): Bitmap? {
  try {
    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val pdfRenderer = PdfRenderer(fileDescriptor)
    if (pdfRenderer.pageCount > pageIndex) {
      val page = pdfRenderer.openPage(pageIndex)
      val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
      page.close()
      pdfRenderer.close()
      fileDescriptor.close()
      return bitmap
    }
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return null
}

fun saveSignedDocument(
  context: Context,
  originalFile: File,
  paths: List<android.graphics.Path>,
  scaleX: Float,
  scaleY: Float,
  inkColor: Color
): File? {
  val isPdf = originalFile.name.endsWith(".pdf")
  val bitmap = if (isPdf) {
    renderPdfPageToBitmap(originalFile)
  } else {
    BitmapFactory.decodeFile(originalFile.absolutePath)
  } ?: return null

  val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
  val canvas = android.graphics.Canvas(mutableBitmap)

  // Configure pen color
  val paintColor = when (inkColor) {
    Color.Blue -> android.graphics.Color.BLUE
    Color.Red -> android.graphics.Color.RED
    Color.Green -> android.graphics.Color.GREEN
    else -> android.graphics.Color.BLACK
  }

  val paint = Paint().apply {
    color = paintColor
    style = Paint.Style.STROKE
    strokeWidth = 8f
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    isAntiAlias = true
  }

  // Draw signature paths
  paths.forEach { path ->
    val scaledPath = android.graphics.Path(path)
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY)
    scaledPath.transform(matrix)
    canvas.drawPath(scaledPath, paint)
  }

  val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
  val name = "Signed_${originalFile.nameWithoutExtension}_$timeStamp.jpg"
  
  val docDir = File(context.filesDir, "scanned_docs").apply {
    if (!exists()) mkdirs()
  }
  val destFile = File(docDir, name)

  try {
    FileOutputStream(destFile).use { out ->
      mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return destFile
  } catch (e: Exception) {
    e.printStackTrace()
    return null
  }
}
