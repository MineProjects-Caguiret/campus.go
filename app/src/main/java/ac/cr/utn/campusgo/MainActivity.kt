package ac.cr.utn.campusgo

import ac.cr.utn.campusgo.data.datasource.local.SharedPreferencesPlaceDataSource
import ac.cr.utn.campusgo.data.repositories.PlaceRepositoryImpl
import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.services.PlaceService
import ac.cr.utn.campusgo.presentation.ALL_CATEGORIES
import ac.cr.utn.campusgo.presentation.PlaceViewModel
import ac.cr.utn.campusgo.presentation.PlaceViewModelFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.UUID
import ac.cr.utn.campusgo.data.datasource.local.InternalPhotoLocalDataSource
import ac.cr.utn.campusgo.data.repositories.PhotoRepositoryImpl
import ac.cr.utn.campusgo.domain.services.PhotoService
import android.widget.ImageView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: PlaceViewModel
    private lateinit var listContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var categoryFilter: Spinner
    private lateinit var emptyMessage: TextView
    private lateinit var photoService: PhotoService


    private val categories = listOf(
        "Todas",
        "Edificio",
        "Biblioteca",
        "Comedor",
        "Laboratorio",
        "Oficina",
        "Deporte",
        "Otro"
    )

    private fun categoryLabels(): List<String> = listOf(
        getString(R.string.category_all),
        getString(R.string.category_building),
        getString(R.string.category_library),
        getString(R.string.category_cafeteria),
        getString(R.string.category_laboratory),
        getString(R.string.category_office),
        getString(R.string.category_sports),
        getString(R.string.category_other)
    )

    private fun categoryLabel(category: String): String {
        val index = categories.indexOf(category)
        return categoryLabels().getOrElse(index) { category }
    }


    private var pendingImageCallback: ((Uri) -> Unit)? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                pendingImageCallback?.invoke(uri)
            } catch (exception: SecurityException) {
                Toast.makeText(
                    this,
                    "No se pudo conservar el acceso a la imagen.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        pendingImageCallback = null
    }

    private val cameraCapture = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                pendingImageCallback?.invoke(photoService.save(bitmap))
            } catch (exception: Exception) {
                Toast.makeText(
                    this,
                    exception.message ?: "No se pudo guardar la fotografía.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        pendingImageCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataSource = SharedPreferencesPlaceDataSource(applicationContext)
        val repository = PlaceRepositoryImpl(dataSource)

        val photoDataSource = InternalPhotoLocalDataSource(applicationContext)
        val photoRepository = PhotoRepositoryImpl(photoDataSource)
        photoService = PhotoService(photoRepository)

        val service = PlaceService(repository, photoService)

        viewModel = ViewModelProvider(
            this,
            PlaceViewModelFactory(service)
        )[PlaceViewModel::class.java]

        buildScreen()
        observeUiState()
    }


    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderPlaces(state.places)

                    state.errorMessage?.let { message ->
                        Toast.makeText(
                            this@MainActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.campus_cream))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "CampusGo"
            subtitle = "Explorá los servicios de tu campus"
            setTitleTextColor(Color.WHITE)
            setSubtitleTextColor(Color.parseColor("#D5E7ED"))
            setBackgroundColor(getColor(R.color.campus_navy))
            setPadding(dp(12), dp(4), dp(12), dp(4))
            elevation = dp(3).toFloat()
        }

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(88)
            )
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        val searchLabel = TextView(this).apply {
            text = "BUSCAR LUGARES"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getColor(R.color.campus_text_secondary))
        }
        content.addView(
            searchLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(7)
            }
        )

        searchInput = EditText(this).apply {
            hint = "Nombre, edificio o servicio..."
            setSingleLine(true)
            textSize = 15f
            setTextColor(getColor(R.color.campus_text_primary))
            setHintTextColor(getColor(R.color.campus_text_secondary))
            setPadding(dp(15), 0, dp(15), 0)
            background = roundedBackground(
                Color.WHITE,
                "#DCE5E8",
                14
            )
            elevation = dp(1).toFloat()
        }

        content.addView(
            searchInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                bottomMargin = dp(18)
            }
        )

        val categoryLabel = TextView(this).apply {
            text = "CATEGORÍA"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getColor(R.color.campus_text_secondary))
        }

        content.addView(
            categoryLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6)
            }
        )

        categoryFilter = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                categoryLabels()
            )
            background = roundedBackground(
                Color.WHITE,
                "#DCE5E8",
                12
            )
            setPadding(dp(10), 0, dp(10), 0)
        }

        content.addView(
            categoryFilter,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                bottomMargin = dp(22)
            }
        )

        val sectionHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val sectionTitle = TextView(this).apply {
            text = "Lugares del campus"
            textSize = 21f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(getColor(R.color.campus_navy))
        }

        sectionHeader.addView(
            sectionTitle,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val sectionAccent = View(this).apply {
            setBackgroundColor(getColor(R.color.campus_amber))
        }

        sectionHeader.addView(
            sectionAccent,
            LinearLayout.LayoutParams(dp(34), dp(4))
        )

        content.addView(
            sectionHeader,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )

        emptyMessage = TextView(this).apply {
            text = "Todavía no hay lugares registrados.\nAgregá el primero para comenzar."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.campus_text_secondary))
            setPadding(dp(16), dp(26), dp(16), dp(26))
            background = roundedBackground(
                Color.WHITE,
                "#DCE5E8",
                16
            )
        }

        content.addView(
            emptyMessage,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, dp(2), 0, dp(4))
        }

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(listContainer)

        content.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val addButton = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonStyle
        ).apply {
            text = "＋  Agregar lugar"
            textSize = 16f
            isAllCaps = false
            cornerRadius = dp(14)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                getColor(R.color.campus_navy)
            )
            setTextColor(Color.WHITE)
            setOnClickListener { showPlaceDialog(null) }
        }

        content.addView(
            addButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(12)
            }
        )

        root.addView(
            content,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        searchInput.addTextChangedListener(SimpleTextWatcher {
            viewModel.updateSearch(searchInput.text.toString())
        })

        categoryFilter.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedCategory = categories[position]
                    viewModel.updateCategory(
                        if (position == 0) ALL_CATEGORIES else selectedCategory
                    )
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit
            }
    }



    private fun renderPlaces(places: List<Place>) {
        listContainer.removeAllViews()
        emptyMessage.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE

        places.forEach { place ->
            val card = MaterialCardView(this).apply {
                radius = dp(18).toFloat()
                cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                strokeWidth = dp(1)
                strokeColor = getColor(R.color.campus_border)
                useCompatPadding = true
            }

            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(15), dp(15), dp(12))
            }

            val name = TextView(this).apply {
                text = place.name
                textSize = 19f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(getColor(R.color.campus_navy))
            }

            content.addView(
                name,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            )

            val category = TextView(this).apply {
                text = categoryLabel(place.category)
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(getColor(R.color.campus_navy))
                setPadding(dp(10), dp(5), dp(10), dp(5))
                background = roundedBackground(
                    Color.parseColor("#E3F1F3"),
                    "#E3F1F3",
                    20
                )
            }

            content.addView(
                category,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            )

            if (place.description.isNotBlank()) {
                val description = TextView(this).apply {
                    text = place.description
                    textSize = 14f
                    setTextColor(getColor(R.color.campus_text_secondary))
                    setLineSpacing(dp(3).toFloat(), 1f)
                }

                content.addView(
                    description,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(10)
                    }
                )
            }

            place.imageUri?.let { value ->
                val image = ImageView(this).apply {
                    contentDescription = getString(R.string.photo_description, place.name)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    adjustViewBounds = true
                    setImageURI(Uri.parse(value))
                    background = roundedBackground(
                        getColor(R.color.campus_cream),
                        getColor(R.color.campus_cream),
                        12
                    )
                }

                content.addView(
                    image,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(180)
                    ).apply {
                        topMargin = dp(4)
                        bottomMargin = dp(12)
                    }
                )
            }

            val separator = View(this).apply {
                setBackgroundColor(getColor(R.color.campus_border))
            }

            content.addView(
                separator,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    bottomMargin = dp(8)
                }
            )

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }

            if (place.latitude != null && place.longitude != null) {
                actions.addView(actionButton(getString(R.string.action_map)) { openMap(place) })
            }

            actions.addView(actionButton(getString(R.string.action_edit)) { showPlaceDialog(place) })
            actions.addView(actionButton(getString(R.string.action_delete)) { confirmDelete(place) })
            content.addView(
                actions,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            card.addView(content)

            listContainer.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            )
        }
    }



    private fun actionButton(label: String, action: () -> Unit): Button {
        val isDelete = label == getString(R.string.action_delete)

        return MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = dp(38)
            minimumHeight = dp(38)
            cornerRadius = dp(10)
            insetTop = 0
            insetBottom = 0
            setPadding(dp(10), 0, dp(10), 0)

            setTextColor(
                getColor(
                    if (isDelete) R.color.campus_error
                    else R.color.campus_navy
                )
            )

            strokeColor = android.content.res.ColorStateList.valueOf(
                getColor(
                    if (isDelete) R.color.campus_error
                    else R.color.campus_border
                )
            )

            setOnClickListener { action() }
        }
    }

    private fun roundedBackground(
        fillColor: Int,
        strokeColor: String,
        cornerRadius: Int
    ): GradientDrawable {
        return roundedBackground(
            fillColor,
            Color.parseColor(strokeColor),
            cornerRadius
        )
    }

    private fun roundedBackground(
        fillColor: Int,
        strokeColor: Int,
        cornerRadius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            setCornerRadius(dp(cornerRadius).toFloat())
            setStroke(dp(1), strokeColor)
        }
    }



    private fun showPlaceDialog(existing: Place?) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }

        fun addLabel(text: String) {
            form.addView(
                TextView(this).apply {
                    this.text = text
                    textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(getColor(R.color.campus_text_secondary))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                    bottomMargin = dp(5)
                }
            )
        }

        fun field(
            hintText: String,
            value: String = "",
            multiline: Boolean = false
        ): EditText {
            val input = EditText(this).apply {
                hint = hintText
                setText(value)
                textSize = 15f
                setTextColor(getColor(R.color.campus_text_primary))
                setHintTextColor(getColor(R.color.campus_text_secondary))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedBackground(
                    Color.WHITE,
                    getColor(R.color.campus_border),
                    12
                )

                if (multiline) {
                    minLines = 2
                    maxLines = 4
                    gravity = Gravity.TOP
                } else {
                    setSingleLine(true)
                }
            }

            form.addView(
                input,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    if (multiline) dp(90) else dp(50)
                )
            )

            return input
        }

        addLabel("NOMBRE DEL LUGAR")
        val nameInput = field("Ej. Biblioteca central", existing?.name.orEmpty())

        addLabel("CATEGORÍA")
        val formCategories = categories.drop(1)
        val formCategoryLabels = categoryLabels().drop(1)

        val categorySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                formCategoryLabels
            )
            background = roundedBackground(
                Color.WHITE,
                getColor(R.color.campus_border),
                12
            )
            setPadding(dp(10), 0, dp(10), 0)

            val index = formCategories.indexOf(existing?.category)
            if (index >= 0) setSelection(index)
        }

        form.addView(
            categorySpinner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        addLabel("DESCRIPCIÓN")
        val descriptionInput = field(
            "Información adicional del lugar",
            existing?.description.orEmpty(),
            multiline = true
        )

        addLabel("COORDENADAS (OPCIONAL)")
        val latitudeInput = field(
            "Latitud",
            existing?.latitude?.toString().orEmpty()
        )
        val longitudeInput = field(
            "Longitud",
            existing?.longitude?.toString().orEmpty()
        )

        addLabel("FOTOGRAFÍA")
        var imageUri = existing?.imageUri

        val imageStatus = TextView(this).apply {
            text = if (imageUri != null) {
                "Fotografía seleccionada"
            } else {
                "No se ha seleccionado una fotografía"
            }
            textSize = 13f
            setTextColor(getColor(R.color.campus_text_secondary))
            setPadding(0, dp(4), 0, dp(8))
        }
        form.addView(imageStatus)

        val galleryButton = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Seleccionar desde galería"
            isAllCaps = false
            cornerRadius = dp(10)
            setOnClickListener {
                pendingImageCallback = { uri ->
                    imageUri = uri.toString()
                    imageStatus.text = "Fotografía seleccionada"
                }
                imagePicker.launch(arrayOf("image/*"))
            }
        }

        form.addView(
            galleryButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val cameraButton = MaterialButton(this).apply {
            text = "Tomar fotografía"
            isAllCaps = false
            cornerRadius = dp(10)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.campus_teal)
                )
            setTextColor(Color.WHITE)

            setOnClickListener {
                pendingImageCallback = { uri ->
                    imageUri = uri.toString()
                    imageStatus.text = "Fotografía capturada"
                }
                cameraCapture.launch(null)
            }
        }

        form.addView(
            cameraButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(8)
            }
        )

        val scroll = ScrollView(this).apply {
            addView(form)
            isFillViewport = false
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (existing == null) "Registrar lugar"
                else "Editar lugar"
            )
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(getColor(R.color.campus_navy))
                setOnClickListener {
                    val name = nameInput.text.toString().trim()
                    val latitudeText = latitudeInput.text.toString().trim()
                    val longitudeText = longitudeInput.text.toString().trim()

                    val latitude = latitudeText.toDoubleOrNull()
                    val longitude = longitudeText.toDoubleOrNull()

                    if (name.isBlank()) {
                        nameInput.error = "Ingresá el nombre del lugar"
                        nameInput.requestFocus()
                        return@setOnClickListener
                    }

                    if ((latitudeText.isNotEmpty() && latitude == null) ||
                        (longitudeText.isNotEmpty() && longitude == null)
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            "Ingresá coordenadas numéricas válidas.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    if ((latitude == null) != (longitude == null)) {
                        Toast.makeText(
                            this@MainActivity,
                            "Ingresá ambas coordenadas o dejá las dos vacías.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    if (latitude != null && latitude !in -90.0..90.0) {
                        latitudeInput.error = "La latitud debe estar entre -90 y 90"
                        return@setOnClickListener
                    }

                    if (longitude != null && longitude !in -180.0..180.0) {
                        longitudeInput.error = "La longitud debe estar entre -180 y 180"
                        return@setOnClickListener
                    }

                    val place = Place(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        category = formCategories[categorySpinner.selectedItemPosition],
                        description = descriptionInput.text.toString().trim(),
                        latitude = latitude,
                        longitude = longitude,
                        imageUri = imageUri
                    )

                    viewModel.savePlace(place)
                    dialog.dismiss()
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                getColor(R.color.campus_text_secondary)
            )
        }

        dialog.show()
    }

    private fun confirmDelete(place: Place) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar lugar")
            .setMessage("¿Deseás eliminar \"${place.name}\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deletePlace(place.id)
            }
            .show()
    }

    private fun openMap(place: Place) {
        val latitude = place.latitude ?: return
        val longitude = place.longitude ?: return
        val label = Uri.encode(place.name)
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No hay una aplicación de mapas instalada.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
