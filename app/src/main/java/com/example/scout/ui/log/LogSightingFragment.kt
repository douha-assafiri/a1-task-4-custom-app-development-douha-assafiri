package com.example.scout.ui.log

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.scout.R
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.databinding.FragmentLogSightingBinding
import com.example.scout.utils.iconicTaxonCategory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogSightingFragment : Fragment() {

    private var _binding: FragmentLogSightingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogSightingViewModel by viewModels()

    private var photoUri: Uri? = null
    private var photoPath: String? = null
    private var selectedCategory: String? = null
    private var selectedTaxonId: Long? = null
    private var lastSelectedName: String? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var searchJob: Job? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else showSnackbar(getString(R.string.camera_permission_denied))
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fetchCurrentLocation() }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) showPhotoPreview(photoUri)
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            photoUri = it
            photoPath = it.toString()
            showPhotoPreview(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogSightingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateTime()
        handlePrefill()
        setupSpeciesSearch()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupDateTime() {
        val fmt = SimpleDateFormat("EEE, d MMM yyyy  h:mm a", Locale.getDefault())
        binding.tvDateTime.text = fmt.format(Date())
    }

    private fun handlePrefill() {
        val prefillName = arguments?.getString("prefillName", "")?.takeIf { it.isNotBlank() }
        val prefillTaxonId = arguments?.getInt("prefillTaxonId", -1)?.takeIf { it > 0 }?.toLong()
        val prefillIconicTaxon = arguments?.getString("prefillIconicTaxon", "")
        if (prefillName != null) {
            binding.etSpeciesName.setText(prefillName)
            selectedTaxonId = prefillTaxonId
            lastSelectedName = prefillName
            selectedCategory = if (iconicTaxonCategory(prefillIconicTaxon) == "Plants") "Plant" else "Animal"
        }
    }

    private fun setupClickListeners() {
        binding.btnCamera.setOnClickListener { launchCameraOrRequest() }
        binding.btnGallery.setOnClickListener { pickImage.launch("image/*") }
        binding.btnEditPhoto.setOnClickListener { showPhotoPickerDialog() }

        binding.tilLocation.setEndIconOnClickListener {
            handleLocationClick()
        }

        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                performSave()
            }
        }
    }

    private fun handleLocationClick() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun performSave() {
        viewModel.save(
            LogSightingViewModel.SightingSubmission(
                speciesName = binding.etSpeciesName.text.toString().trim(),
                category = selectedCategory!!,
                photoPath = photoPath,
                latitude = selectedLatitude,
                longitude = selectedLongitude,
                locationName = binding.etLocation.text?.toString()?.takeIf { it.isNotBlank() },
                notes = binding.etNotes.text?.toString()?.takeIf { it.isNotBlank() },
                taxonId = selectedTaxonId
            )
        )
    }

    private fun observeViewModel() {
        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Snackbar.make(binding.root, getString(R.string.sighting_logged), Snackbar.LENGTH_SHORT).show()
                clearForm()
            }
        }

        viewModel.locationResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                handleLocationResult(result)
            } else {
                showSnackbar("Location not found")
            }
        }
    }

    private fun handleLocationResult(result: LogSightingViewModel.LocationResult) {
        selectedLatitude = result.latitude
        selectedLongitude = result.longitude
        binding.etLocation.setText(result.name)
    }

    private fun setupSpeciesSearch() {
        val adapter = object : ArrayAdapter<TaxonResult>(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val item = getItem(position)
                (v as? android.widget.TextView)?.text = buildString {
                    append(item?.commonName ?: item?.name ?: "")
                    item?.name?.let { if (item.commonName != null) append("  ·  $it") }
                }
                return v
            }
            override fun getFilter(): Filter = object : Filter() {
                override fun performFiltering(c: CharSequence?) = FilterResults()
                override fun publishResults(c: CharSequence?, r: FilterResults?) {
                    // results are published via adapter.addAll() in text watcher
                }
            }
        }
        binding.etSpeciesName.setAdapter(adapter)

        binding.etSpeciesName.addTextChangedListener { text ->
            val query = text.toString()
            if (query == lastSelectedName) return@addTextChangedListener
            selectedTaxonId = null
            searchJob?.cancel()
            if (query.length < 2) { adapter.clear(); return@addTextChangedListener }
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(400)
                val results = runCatching {
                    viewModel.speciesRepo.searchTaxa(query).results.take(6)
                }.getOrDefault(emptyList())
                adapter.clear()
                adapter.addAll(results)
                adapter.notifyDataSetChanged()
                binding.etSpeciesName.showDropDown()
            }
        }

        binding.etSpeciesName.setOnItemClickListener { _, _, position, _ ->
            val species = adapter.getItem(position) ?: return@setOnItemClickListener
            selectedTaxonId = species.id
            lastSelectedName = species.commonName ?: species.name
            binding.etSpeciesName.setText(lastSelectedName)
            selectedCategory = if (iconicTaxonCategory(species.iconicTaxonName) == "Plants") "Plant" else "Animal"
            binding.tilSpeciesName.error = null
        }
    }

    private fun launchCameraOrRequest() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) launchCamera()
        else requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun showPhotoPickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.sighting_photo_title)
            .setItems(arrayOf(getString(R.string.photo_picker_camera), getString(R.string.photo_picker_gallery))) { _, which ->
                if (which == 0) launchCameraOrRequest() else pickImage.launch("image/*")
            }
            .show()
    }

    private fun showPhotoPreview(uri: Uri?) {
        if (uri == null) return
        binding.photoPreviewContainer.isVisible = true
        binding.ivPhoto.load(uri)
    }

    private fun launchCamera() {
        val file = File.createTempFile("sighting_", ".jpg", requireContext().cacheDir)
        photoPath = file.absolutePath
        photoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        takePicture.launch(photoUri!!)
    }

    private fun fetchCurrentLocation() {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        
        // try last known first for speed
        val location = lm.getLastKnownLocation(provider)
        if (location != null && (System.currentTimeMillis() - location.time < 60000)) {
            viewModel.resolveAddress(location.latitude, location.longitude)
        } else {
            // request fresh location
            showSnackbar("Fetching fresh location...")
            
            lateinit var locationListener: android.location.LocationListener
            locationListener = android.location.LocationListener { loc ->
                viewModel.resolveAddress(loc.latitude, loc.longitude)
                lm.removeUpdates(locationListener)
            }
            lm.requestLocationUpdates(provider, 0L, 0f, locationListener)
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        when {
            binding.etSpeciesName.text.isNullOrBlank() -> {
                binding.tilSpeciesName.error = getString(R.string.species_name_error)
                valid = false
            }
            selectedTaxonId == null || selectedCategory == null -> {
                binding.tilSpeciesName.error = getString(R.string.species_select_error)
                valid = false
            }
            else -> binding.tilSpeciesName.error = null
        }

        return valid
    }

    private fun clearForm() {
        binding.etSpeciesName.setText("")
        binding.etLocation.setText("")
        binding.etNotes.setText("")
        binding.ivPhoto.setImageDrawable(null)
        binding.photoPreviewContainer.isVisible = false
        selectedCategory = null
        selectedTaxonId = null
        lastSelectedName = null
        selectedLatitude = null
        selectedLongitude = null
        photoUri = null
        photoPath = null
    }

    private fun showSnackbar(msg: String) =
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}