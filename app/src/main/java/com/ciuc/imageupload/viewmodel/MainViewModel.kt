package com.ciuc.imageupload.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel:ViewModel() {
    private val _imageUploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Empty)
    val imageUploadState = _imageUploadState.asStateFlow()

    suspend fun uploadImage(path:String){
        _imageUploadState.value = ImageUploadState.Loading
        delay(2000)
        _imageUploadState.value = ImageUploadState.Success
    }
}

sealed class ImageUploadState{
    data object Loading:ImageUploadState()
    data object Empty:ImageUploadState()
    data object Success:ImageUploadState()
    data object Failure:ImageUploadState()

}