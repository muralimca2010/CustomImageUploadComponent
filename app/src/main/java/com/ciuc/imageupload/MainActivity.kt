package com.ciuc.imageupload

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ciuc.imageupload.component.ImageUploadComponent
import com.ciuc.imageupload.viewmodel.ImageUploadState
import com.ciuc.imageupload.viewmodel.MainViewModel
import com.ciuc.imageupload.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(),ImageUploadComponent.ImageUploadListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel:MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        binding.imageUploadComponent.initRegisterForResult(this)
        binding.imageUploadComponent.setImageUploadListener(this)
        observeState()
    }

    private fun observeState(){
        lifecycleScope.launch {
            mainViewModel.imageUploadState.collect{
                when(it){
                    is ImageUploadState.Loading -> {
                        binding.isProgressShown = true
                    }
                    is ImageUploadState.Success -> {
                        binding.isProgressShown = false
                        Toast.makeText(binding.root.context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onImageSubmit(imageUri: Uri?) {
         lifecycleScope.launch {
                 binding.isProgressShown = true
                 imageUri?.path?.let { mainViewModel.uploadImage(it) }
         }
    }
}