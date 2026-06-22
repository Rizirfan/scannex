package com.example.docscanner.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.docscanner.DocumentManager
import com.example.docscanner.ScannedDoc
import com.example.docscanner.getDisplayName
import com.example.docscanner.Sign
import com.example.docscanner.data.DefaultDataRepository
import com.example.docscanner.theme.DocScannerTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val documentManager = remember { DocumentManager(context.applicationContext) }
  val repository = remember { DefaultDataRepository(documentManager) }
  val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(repository) }

  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.refresh()
  }

  val scannerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartIntentSenderForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
      val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data!!)
      if (scanningResult != null) {
        val pdf = scanningResult.pdf
        val pages = scanningResult.pages
        
        var savedCount = 0
        if (pdf != null) {
          val savedPdf = documentManager.savePdf(pdf.uri)
          if (savedPdf != null) savedCount++
        }
        if (!pages.isNullOrEmpty()) {
          val savedJpegs = documentManager.saveJpegs(pages.map { it.imageUri })
          savedCount += savedJpegs.size
        }
        
        if (savedCount > 0) {
          Toast.makeText(context, "Saved $savedCount files", Toast.LENGTH_SHORT).show()
        }
        viewModel.refresh()
      }
    }
  }

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      val imported = documentManager.importDocument(uri)
      if (imported != null) {
        Toast.makeText(context, "Imported successfully", Toast.LENGTH_SHORT).show()
        viewModel.refresh()
      } else {
        Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val scannerOptions = remember {
    GmsDocumentScannerOptions.Builder()
      .setGalleryImportAllowed(true)
      .setResultFormats(
        GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
      )
      .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
      .build()
  }

  val scannerClient = remember {
    GmsDocumentScanning.getClient(scannerOptions)
  }

  val onScanClick: () -> Unit = {
    val activity = context.findActivity()
    if (activity != null) {
      scannerClient.getStartScanIntent(activity)
        .addOnSuccessListener { intentSender ->
          scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
        .addOnFailureListener { e ->
          Toast.makeText(context, "Failed to start scanner: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    } else {
      Toast.makeText(context, "Activity context not found", Toast.LENGTH_SHORT).show()
    }
  }

  when (state) {
    MainScreenUiState.Loading -> {
      Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
      }
    }
    is MainScreenUiState.Success -> {
      MainScreen(
        documents = (state as MainScreenUiState.Success).data,
        documentManager = documentManager,
        onScanClick = onScanClick,
        onImportClick = { filePickerLauncher.launch("*/*") },
        onItemClick = onItemClick,
        onDeleteDoc = { doc ->
          documentManager.deleteDocument(doc)
          viewModel.refresh()
        },
        modifier = modifier
      )
    }
    is MainScreenUiState.Error -> {
      Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Text("Error loading data: ${(state as MainScreenUiState.Error).throwable.message}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
  documents: List<ScannedDoc>,
  documentManager: DocumentManager,
  onScanClick: () -> Unit,
  onImportClick: () -> Unit,
  onItemClick: (NavKey) -> Unit,
  onDeleteDoc: (ScannedDoc) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var docToDelete by remember { mutableStateOf<ScannedDoc?>(null) }
  var selectedFilter by remember { mutableStateOf("All") }

  val filteredDocs = remember(searchQuery, selectedFilter, documents) {
    var list = documents
    if (searchQuery.isNotEmpty()) {
      list = list.filter { it.getDisplayName().contains(searchQuery, ignoreCase = true) }
    }
    when (selectedFilter) {
      "PDF" -> list.filter { it.fileType == "PDF" }
      "Image" -> list.filter { it.fileType == "JPEG" && !it.name.startsWith("Signed_") }
      "Signed" -> list.filter { it.name.startsWith("Signed_") }
      "Imported" -> list.filter { it.name.startsWith("Imported_") }
      else -> list
    }
  }

  val totalCount = documents.size
  val pdfCount = remember(documents) { documents.count { it.fileType == "PDF" } }
  val imgCount = remember(documents) { documents.count { it.fileType == "JPEG" && !it.name.startsWith("Signed_") } }
  val signedCount = remember(documents) { documents.count { it.name.startsWith("Signed_") } }
  val importedCount = remember(documents) { documents.count { it.name.startsWith("Imported_") } }

  if (docToDelete != null) {
    AlertDialog(
      onDismissRequest = { docToDelete = null },
      title = { Text("Delete scan?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = { Text("This will permanently delete this document.", fontSize = 13.sp) },
      confirmButton = {
        TextButton(
          onClick = {
            docToDelete?.let { onDeleteDoc(it) }
            docToDelete = null
          }
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      },
      dismissButton = {
        TextButton(onClick = { docToDelete = null }) {
          Text("Cancel", fontSize = 13.sp)
        }
      }
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Compact Monochrome Import FAB
        FloatingActionButton(
          onClick = onImportClick,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(44.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Upload, contentDescription = "Import", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("IMPORT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        // Compact Monochrome Scan FAB
        FloatingActionButton(
          onClick = onScanClick,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(44.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Add, contentDescription = "Scan Icon", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SCAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues)
    ) {
      
      // Compact Minimalist Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "SCANNEX",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
              text = "$totalCount scans • $signedCount signed",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.secondary
            )
          }

          // SCANNEX Logo
          Image(
            painter = painterResource(id = com.example.docscanner.R.drawable.scannex_logo),
            contentDescription = "SCANNEX Logo",
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Compact Search Bar (40.dp height)
        GoogleSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tiny Filter Chips Row
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            FilterChipItem(name = "All ($totalCount)", selected = selectedFilter == "All") { selectedFilter = "All" }
          }
          item {
            FilterChipItem(name = "PDFs ($pdfCount)", selected = selectedFilter == "PDF") { selectedFilter = "PDF" }
          }
          item {
            FilterChipItem(name = "Images ($imgCount)", selected = selectedFilter == "Image") { selectedFilter = "Image" }
          }
          item {
            FilterChipItem(name = "Imported ($importedCount)", selected = selectedFilter == "Imported") { selectedFilter = "Imported" }
          }
          item {
            FilterChipItem(name = "Signed ($signedCount)", selected = selectedFilter == "Signed") { selectedFilter = "Signed" }
          }
        }
      }

      // Main Files List
      if (filteredDocs.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (searchQuery.isNotEmpty() || selectedFilter != "All") "No matches" else "No scans",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 80.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredDocs, key = { it.file.absolutePath }) { doc ->
            DocumentItem(
              doc = doc,
              onOpen = { openFile(context, doc, documentManager) },
              onShare = { shareFile(context, doc, documentManager) },
              onSaveToGallery = if (doc.fileType == "JPEG") {
                {
                  val savedUri = documentManager.saveImageToGallery(doc)
                  if (savedUri != null) {
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                  }
                }
              } else null,
              onSign = { onItemClick(Sign(doc.file.absolutePath)) },
              onDelete = { docToDelete = doc }
            )
          }
        }
      }
    }
  }
}

@Composable
fun FilterChipItem(
  name: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  val backgroundColor = if (selected) {
    MaterialTheme.colorScheme.onBackground
  } else {
    MaterialTheme.colorScheme.background
  }
  val contentColor = if (selected) {
    MaterialTheme.colorScheme.background
  } else {
    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
  }

  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() },
    color = backgroundColor,
    contentColor = contentColor,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Text(
      text = name,
      fontSize = 11.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
  }
}

@Composable
fun GoogleSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(40.dp),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.background,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier.weight(1.0f),
        contentAlignment = Alignment.CenterStart
      ) {
        if (query.isEmpty()) {
          Text(
            text = "Search...",
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
            fontSize = 13.sp
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
          ),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
      }
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.size(20.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}

@Composable
fun DocumentItem(
  doc: ScannedDoc,
  onOpen: () -> Unit,
  onShare: () -> Unit,
  onSaveToGallery: (() -> Unit)? = null,
  onSign: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isSigned = doc.name.startsWith("Signed_")

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onOpen() },
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline) // Simple B&W border
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      
      // Compact Thumbnail (48.dp)
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        if (doc.fileType == "JPEG") {
          val bitmap = rememberImageBitmap(doc.file)
          if (bitmap != null) {
            Image(
              bitmap = bitmap,
              contentDescription = "Thumbnail",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Icon(
              imageVector = Icons.Default.Image,
              contentDescription = "Image",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp)
            )
          }
        } else {
          Text(
            text = "PDF",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Content Metadata
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = doc.getDisplayName(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
            color = MaterialTheme.colorScheme.onSurface
          )
          if (isSigned) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
              modifier = Modifier
                .background(Color.Transparent, RoundedCornerShape(2.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
              Text(
                text = "SIGNED",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
          text = "${doc.fileType} • ${doc.formattedSize} • ${doc.formattedDate}",
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
          text = "Address: ${doc.file.absolutePath}",
          fontSize = 8.sp,
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Miniature Actions (28.dp buttons with 16.dp icons)
      Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (onSaveToGallery != null) {
          IconButton(
            onClick = onSaveToGallery,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = "Save",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        IconButton(
          onClick = onSign,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Create,
            contentDescription = "Sign",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
          )
        }

        IconButton(
          onClick = onShare,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun EmptyState(
  onScanClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = Icons.Default.Menu,
      contentDescription = "Empty",
      modifier = Modifier.size(36.dp),
      tint = MaterialTheme.colorScheme.secondary
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = "No Scans Yet",
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Scan documents and draw signatures locally.",
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.secondary,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = onScanClick,
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      ),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Icon(imageVector = Icons.Default.Add, contentDescription = "Scan", modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("SCAN NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun rememberImageBitmap(file: File): ImageBitmap? {
  var bitmap by remember(file) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(file) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      try {
        val options = BitmapFactory.Options().apply {
          inSampleSize = 4
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          bitmap = decoded
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
  return bitmap
}

fun Context.findActivity(): Activity? {
  var currentContext = this
  while (currentContext is android.content.ContextWrapper) {
    if (currentContext is Activity) {
      return currentContext
    }
    currentContext = currentContext.baseContext
  }
  return null
}

fun openFile(context: Context, doc: ScannedDoc, documentManager: DocumentManager) {
  val uri = documentManager.getShareUri(doc)
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, if (doc.fileType == "PDF") "application/pdf" else "image/jpeg")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try {
    context.startActivity(Intent.createChooser(intent, "Open with"))
  } catch (e: Exception) {
    Toast.makeText(context, "No application found to open this file", Toast.LENGTH_SHORT).show()
  }
}

fun shareFile(context: Context, doc: ScannedDoc, documentManager: DocumentManager) {
  val uri = documentManager.getShareUri(doc)
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = if (doc.fileType == "PDF") "application/pdf" else "image/jpeg"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  try {
    context.startActivity(Intent.createChooser(intent, "Share scan"))
  } catch (e: Exception) {
    Toast.makeText(context, "Sharing failed", Toast.LENGTH_SHORT).show()
  }
}
