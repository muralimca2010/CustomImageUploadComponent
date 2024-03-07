package com.ciuc.imageupload.component

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import com.ciuc.imageupload.R
import com.ciuc.imageupload.databinding.ImageUploadComponentBinding

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class ImageUploadComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ImageUploadComponentBinding
    private lateinit var activity: FragmentActivity
    private val cameraPermission = Manifest.permission.CAMERA
    private val galleryPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private var imageUploadListener: ImageUploadListener? = null

    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<PickVisualMediaRequest>


    private var selectedImageUri: Uri? = null
    private var capturedImageURI: Uri? = null


    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = DataBindingUtil.inflate(inflater, R.layout.image_upload_component, this, true)

        binding.btnSelectImage.setOnClickListener {
            showImageSelectDialog()
        }

        binding.btnPreview.setOnClickListener {
            previewImage()
        }

        binding.btnSubmit.setOnClickListener {
            if (selectedImageUri==null) showToast("please select image to submit") else uploadImage()
        }

    }

    fun initRegisterForResult(activity: FragmentActivity) {
        this.activity = activity

        cameraPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
                if (permission) {
                    captureImage()
                } else {
                    showToast("Camera Permission Denied")
                }
            }


        galleryPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
                if (permission) {
                    openGalleryForImages()
                } else {
                    showToast("Gallery Permission Denied")
                }
            }


        galleryLauncher =
            activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    selectedImageUri = uri
                    updateFileInfo()
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }


        cameraLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    selectedImageUri = capturedImageURI
                    updateFileInfo()
                } else {
                    showToast("Image Capture Cancelled")
                }
            }
    }

    private fun uploadImage(){
        selectedImageUri?.let {
            imageUploadListener?.onImageSubmit(selectedImageUri)
        }
    }

    fun setImageUploadListener(listener: ImageUploadListener) {
        this.imageUploadListener = listener
    }

    @SuppressLint("SetTextI18n")
    private fun updateFileInfo() {
        binding.shouldPreviewVisible = true
        val (fileName, fileType) = getFileNameAndType(selectedImageUri)
        binding.tvFileName.text = "Selected File: $fileName"
        binding.tvFileType.text = "Selected File Type: $fileType"
    }

    private fun getFileNameAndType(uri: Uri?): Pair<String, String> {
        val cursor = uri?.let {
            context.contentResolver.query(it, null, null, null, null)
        }

        val fileNameWithExtension = cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: "Unknown"

        val fileName = fileNameWithExtension.substringBeforeLast('.', "")
        val fileType = fileNameWithExtension.substringAfterLast('.', "")

        return Pair(fileName, fileType)
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showImageSelectDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(context)
            .setTitle("Select Image From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        if (checkPermission()) captureImage() else requestCameraPermission()
    }

    private fun captureImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file: File = createImageFile()

        capturedImageURI = FileProvider.getUriForFile(
            context,
            "com.ciuc.imageupload.fileProvider",
            file
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageURI)
        cameraLauncher.launch(intent)
    }


    private fun previewImage() {
        val view = LayoutInflater.from(context).inflate(R.layout.image_preview, null)
        val image: ImageView = view.findViewById(R.id.img_preview)
        selectedImageUri.let {
            image.setImageURI(it)
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            .setView(view)
            .setTitle("Image Preview")
            .setPositiveButton("Close") { _, _ -> }

        val previewDialog = builder.create()
        previewDialog.show()
        val closeButton = previewDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        closeButton.setOnClickListener {
            previewDialog.dismiss()
        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "IMG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun openGallery() {
        if (checkPermission()) openGalleryForImages() else requestGalleryPermission()
    }

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            context,
            cameraPermission
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    galleryPermission
                ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(cameraPermission)
    }

    private fun requestGalleryPermission() {
        galleryPermissionLauncher.launch(galleryPermission)
    }


    private fun openGalleryForImages() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    interface ImageUploadListener {
        fun onImageSubmit(imageUri: Uri?)
    }

}