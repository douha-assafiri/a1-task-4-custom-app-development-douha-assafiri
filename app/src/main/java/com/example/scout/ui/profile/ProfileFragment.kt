package com.example.scout.ui.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.scout.R
import com.example.scout.databinding.FragmentProfileBinding
import com.example.scout.utils.LevelSystem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var prefs: android.content.SharedPreferences
    private var cameraPhotoUri: Uri? = null

    companion object {
        private const val TEMP_PROFILE_PHOTO = "profile_temp.jpg"
        private const val PERMANENT_PROFILE_PHOTO = "profile_picture.jpg"
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    private val takeProfilePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraPhotoUri?.toString()?.let { saveProfilePhoto(it) }
    }

    private val pickProfileImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            runCatching {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            saveProfilePhoto(it.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("scout_prefs", Context.MODE_PRIVATE)

        setupName()
        setupAvatar()
        setupLevelAndStats()
        setupRecentSightings()

        binding.btnSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_sightings)
        }
    }

    private fun setupLevelAndStats() {
        viewModel.sightingsCount.observe(viewLifecycleOwner) { count ->
            binding.tvSightingsCount.text = (count ?: 0).toString()
            val debugOverride = prefs.getInt("debug_sightings_override", -1)
            updateLevel(if (debugOverride >= 0) debugOverride else count ?: 0)
        }

        binding.tvLevelTitle.setOnLongClickListener {
            showDebugOverrideDialog()
            true
        }

        viewModel.favouritesCount.observe(viewLifecycleOwner) { count ->
            binding.tvFavouritesCount.text = (count ?: 0).toString()
        }

        viewModel.distinctSpeciesCount.observe(viewLifecycleOwner) { count ->
            binding.tvSpeciesCount.text = (count ?: 0).toString()
        }
    }

    private fun showDebugOverrideDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.debug_override_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            val current = prefs.getInt("debug_sightings_override", -1)
            if (current >= 0) setText(current.toString())
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.debug_override_title)
            .setView(input)
            .setPositiveButton(R.string.debug_apply) { _, _ ->
                val v = input.text.toString().toIntOrNull() ?: -1
                prefs.edit().putInt("debug_sightings_override", v).apply()
                updateLevel(if (v >= 0) v else viewModel.sightingsCount.value ?: 0)
            }
            .setNeutralButton(R.string.debug_clear_override) { _, _ ->
                prefs.edit().remove("debug_sightings_override").apply()
                updateLevel(viewModel.sightingsCount.value ?: 0)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupRecentSightings() {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        viewModel.recentSightings.observe(viewLifecycleOwner) { sightings ->
            binding.recentSightingsContainer.removeAllViews()
            sightings.forEach { sighting ->
                val itemView = layoutInflater.inflate(R.layout.item_recent_sighting, binding.recentSightingsContainer, false)
                bindRecentSightingItem(itemView, sighting, fmt)
                binding.recentSightingsContainer.addView(itemView)
            }
        }
    }

    private fun bindRecentSightingItem(view: View, sighting: com.example.scout.data.db.entities.SightingEntity, fmt: SimpleDateFormat) {
        view.findViewById<android.widget.TextView>(R.id.tvSightingName).text = sighting.speciesName
        view.findViewById<android.widget.TextView>(R.id.tvSightingDate).text = fmt.format(Date(sighting.loggedAt))

        val thumb = view.findViewById<ImageView>(R.id.ivActivityThumb)
        if (!sighting.photoPath.isNullOrBlank()) {
            thumb.imageTintList = null
            thumb.load(File(sighting.photoPath))
        } else {
            val icon = if (sighting.category == "Plant") R.drawable.ic_explore else R.drawable.ic_log
            thumb.setImageResource(icon)
            thumb.imageTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.green_primary)
            )
        }
    }

    private fun setupName() {
        binding.tvName.text = prefs.getString("user_name", getString(R.string.default_user_name))

        binding.btnEditName.setOnClickListener {
            val input = EditText(requireContext()).apply {
                setText(binding.tvName.text)
                setSingleLine()
            }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_name_title)
                .setView(input)
                .setPositiveButton(R.string.save) { _, _ ->
                    val newName = input.text.toString().trim().ifEmpty { getString(R.string.default_user_name) }
                    prefs.edit().putString("user_name", newName).apply()
                    binding.tvName.text = newName
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun setupAvatar() {
        prefs.getString("profile_photo_uri", null)?.let { loadAvatarPhoto(it) }

        binding.avatarContainer.setOnClickListener { showPhotoPickerDialog() }
    }

    private fun showPhotoPickerDialog() {
        val hasPhoto = prefs.getString("profile_photo_uri", null) != null
        val options = buildList {
            add(getString(R.string.photo_picker_camera))
            add(getString(R.string.photo_picker_gallery))
            if (hasPhoto) add(getString(R.string.photo_picker_remove))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.photo_picker_title))
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    getString(R.string.photo_picker_camera) -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    getString(R.string.photo_picker_gallery) -> pickProfileImage.launch("image/*")
                    getString(R.string.photo_picker_remove) -> removeProfilePhoto()
                }
            }
            .show()
    }

    private fun launchCamera() {
        val file = File(requireContext().cacheDir, TEMP_PROFILE_PHOTO)
        cameraPhotoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        takeProfilePicture.launch(cameraPhotoUri!!)
    }

    private fun saveProfilePhoto(uriString: String) {
        // if it was from camera, move it to internal files for persistence
        val finalUri = if (uriString.contains(TEMP_PROFILE_PHOTO)) {
            val tempFile = File(requireContext().cacheDir, TEMP_PROFILE_PHOTO)
            val permanentFile = File(requireContext().filesDir, PERMANENT_PROFILE_PHOTO)
            tempFile.copyTo(permanentFile, overwrite = true)
            Uri.fromFile(permanentFile).toString()
        } else {
            uriString
        }
        
        prefs.edit().putString("profile_photo_uri", finalUri).apply()
        loadAvatarPhoto(finalUri)
    }

    private fun removeProfilePhoto() {
        prefs.edit().remove("profile_photo_uri").apply()
        binding.ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
        binding.ivAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
            requireContext().getColor(R.color.green_primary)
        )
    }

    private fun loadAvatarPhoto(uriString: String) {
        binding.ivAvatar.imageTintList = null
        binding.ivAvatar.load(Uri.parse(uriString)) {
            error(R.drawable.ic_profile_placeholder)
        }
    }

    private fun updateLevel(sightingsCount: Int) {
        val level = LevelSystem.forSightings(sightingsCount)
        val canCustomise = LevelSystem.canCustomiseTitle(sightingsCount)

        val customTitle = if (canCustomise) prefs.getString("custom_title", null) else null
        val displayTitle = customTitle ?: getString(level.titleRes)
        binding.tvLevelTitle.text = getString(R.string.level_format, level.number, displayTitle)

        val progress = LevelSystem.progressToNext(sightingsCount)
        if (progress != null) {
            binding.tvLevelProgress.isVisible = true
            binding.tvLevelProgress.text = getString(R.string.level_progress, progress.first, progress.second, level.number + 1)
        } else {
            binding.tvLevelProgress.isVisible = false
        }

        if (canCustomise) {
            binding.tvLevelTitle.setOnClickListener { showCustomTitleDialog(level.titleRes) }
        } else {
            binding.tvLevelTitle.setOnClickListener(null)
            binding.tvLevelTitle.isClickable = false
        }
    }

    private fun showCustomTitleDialog(@androidx.annotation.StringRes defaultTitleRes: Int) {
        val current = prefs.getString("custom_title", null) ?: getString(defaultTitleRes)
        val input = EditText(requireContext()).apply {
            setText(current)
            setSingleLine()
            hint = getString(R.string.edit_title_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.custom_title_dialog_title)
            .setMessage(R.string.custom_title_unlocked)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newTitle = input.text.toString().trim().ifEmpty { getString(defaultTitleRes) }
                prefs.edit().putString("custom_title", newTitle).apply()
                val sightings = viewModel.sightingsCount.value ?: 0
                updateLevel(sightings)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}