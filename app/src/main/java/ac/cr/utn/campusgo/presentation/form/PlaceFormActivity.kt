package ac.cr.utn.campusgo.presentation.form

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ac.cr.utn.campusgo.CampusGoApplication
import ac.cr.utn.campusgo.R
import ac.cr.utn.campusgo.SimpleTextWatcher
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PlaceFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLACE_ID = "ac.cr.utn.campusgo.EXTRA_PLACE_ID"
    }

    private lateinit var viewModel: PlaceFormViewModel

    private lateinit var titleText: TextView
    private lateinit var nameInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var descriptionInput: EditText
    private lateinit var latitudeInput: EditText
    private lateinit var longitudeInput: EditText
    private lateinit var photoStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var galleryButton: MaterialButton
    private lateinit var cameraButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var saveButton: MaterialButton

    private val categoryValues = listOf(
        "Edificio",
        "Biblioteca",
        "Comedor",
        "Laboratorio",
        "Oficina",
        "Deporte",
        "Otro"
    )

    private val categoryLabels by lazy {
        listOf(
            getString(R.string.category_building),
            getString(R.string.category_library),
            getString(R.string.category_cafeteria),
            getString(R.string.category_laboratory),
            getString(R.string.category_office),
            getString(R.string.category_sports),
            getString(R.string.category_other)
        )
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.updateImageUri(uri.toString())
            } catch (exception: SecurityException) {
                showMessage(getString(R.string.error_image_permission))
            }
        }
    }

    private val cameraCapture = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let(viewModel::savePhoto)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_form)

        initializeViews()
        initializeViewModel()
        configureCategorySpinner()
        configureTextWatchers()
        configureActions()
        observeUiState()

        intent.getStringExtra(EXTRA_PLACE_ID)?.let(viewModel::loadPlace)
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.form_title)
        nameInput = findViewById(R.id.input_place_name)
        categorySpinner = findViewById(R.id.input_place_category)
        descriptionInput = findViewById(R.id.input_place_description)
        latitudeInput = findViewById(R.id.input_latitude)
        longitudeInput = findViewById(R.id.input_longitude)
        photoStatus = findViewById(R.id.text_photo_status)
        progressBar = findViewById(R.id.form_progress)
        galleryButton = findViewById(R.id.button_select_gallery)
        cameraButton = findViewById(R.id.button_take_photo)
        cancelButton = findViewById(R.id.button_cancel)
        saveButton = findViewById(R.id.button_save)
    }

    private fun initializeViewModel() {
        val container = (application as CampusGoApplication).container

        val factory = PlaceFormViewModelFactory(
            container.placeService,
            container.photoService
        )

        viewModel = ViewModelProvider(this, factory)[PlaceFormViewModel::class.java]
    }

    private fun configureCategorySpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryLabels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        categorySpinner.adapter = adapter

        categorySpinner.setOnItemSelectedListener(
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    categoryValues.getOrNull(position)?.let(viewModel::updateCategory)
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit
            }
        )
    }

    private fun configureTextWatchers() {
        nameInput.addTextChangedListener(
            SimpleTextWatcher {
                viewModel.updateName(nameInput.text.toString())
            }
        )

        descriptionInput.addTextChangedListener(
            SimpleTextWatcher {
                viewModel.updateDescription(descriptionInput.text.toString())
            }
        )

        latitudeInput.addTextChangedListener(
            SimpleTextWatcher {
                viewModel.updateLatitude(latitudeInput.text.toString())
            }
        )

        longitudeInput.addTextChangedListener(
            SimpleTextWatcher {
                viewModel.updateLongitude(longitudeInput.text.toString())
            }
        )
    }

    private fun configureActions() {
        galleryButton.setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        cameraButton.setOnClickListener {
            cameraCapture.launch(null)
        }

        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        saveButton.setOnClickListener {
            viewModel.savePlace()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: PlaceFormUiState) {
        val isEditing = intent.getStringExtra(EXTRA_PLACE_ID) != null

        titleText.setText(
            if (isEditing) R.string.action_update else R.string.action_register
        )

        if (nameInput.text.toString() != state.name) {
            nameInput.setText(state.name)
        }

        if (descriptionInput.text.toString() != state.description) {
            descriptionInput.setText(state.description)
        }

        if (latitudeInput.text.toString() != state.latitude) {
            latitudeInput.setText(state.latitude)
        }

        if (longitudeInput.text.toString() != state.longitude) {
            longitudeInput.setText(state.longitude)
        }

        val categoryPosition = categoryValues.indexOf(state.category)
        if (categoryPosition >= 0 &&
            categorySpinner.selectedItemPosition != categoryPosition
        ) {
            categorySpinner.setSelection(categoryPosition)
        }

        photoStatus.setText(
            if (state.imageUri != null) {
                R.string.photo_selected
            } else {
                R.string.photo_not_selected
            }
        )

        progressBar.visibility = if (state.isLoading) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }

        saveButton.isEnabled = !state.isLoading
        galleryButton.isEnabled = !state.isLoading
        cameraButton.isEnabled = !state.isLoading

        state.errorMessage?.let { message ->
            showMessage(message)
            viewModel.clearError()
        }

        if (state.isSaved) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}