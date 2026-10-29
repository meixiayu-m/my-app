package com.calorielens.app.ui.recognition

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.calorielens.app.domain.FoodItem
import com.calorielens.app.recognition.DirectZhipuFoodRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RecognitionUiState(
    val isRecognizing: Boolean = false,
    val isRefining: Boolean = false,
    val items: List<FoodItem>? = null,
    val comment: String = "",
    val imageUri: Uri? = null,
    val stage: String = "正在准备照片…",
    val error: String? = null
)

class RecognitionViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val recognizer = DirectZhipuFoodRecognizer()
    private var recognitionJob: Job? = null
    private val interruptedUri = savedStateHandle.get<String>(KEY_IMAGE)?.let(Uri::parse)
        ?.takeIf { savedStateHandle.get<Boolean>(KEY_IN_PROGRESS) == true }
    private val _state = MutableStateFlow(
        RecognitionUiState(
            imageUri = interruptedUri,
            error = interruptedUri?.let { "上次识别被系统中断，可以点击重试。" }
        )
    )
    val state: StateFlow<RecognitionUiState> = _state.asStateFlow()

    fun recognize(imageUri: Uri) {
        if (_state.value.isRecognizing) return
        recognitionJob?.cancel()
        savedStateHandle[KEY_IMAGE] = imageUri.toString()
        savedStateHandle[KEY_IN_PROGRESS] = true
        _state.value = RecognitionUiState(
            isRecognizing = true,
            imageUri = imageUri,
            stage = WAIT_LINES.first()
        )
        recognitionJob = viewModelScope.launch {
            val stageJob = launch {
                var index = kotlin.random.Random.nextInt(WAIT_LINES.size)
                while (true) {
                    _state.update { it.copy(stage = WAIT_LINES[index % WAIT_LINES.size]) }
                    index++
                    delay(2300)
                }
            }
            try {
                val result = recognizer.recognize(getApplication(), imageUri)
                savedStateHandle[KEY_IN_PROGRESS] = false
                _state.update {
                    it.copy(
                        isRecognizing = false,
                        items = result.items,
                        comment = result.comment,
                        error = null
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                savedStateHandle[KEY_IN_PROGRESS] = false
                _state.update {
                    it.copy(
                        isRecognizing = false,
                        error = error.message ?: "识别失败，请检查手机网络和智谱设置"
                    )
                }
            } finally {
                stageJob.cancel()
            }
        }
    }

    fun retry() {
        _state.value.imageUri?.let(::recognize)
    }

    fun refine(imageUri: Uri, items: List<FoodItem>, instruction: String) {
        if (_state.value.isRefining || instruction.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isRefining = true, error = null) }
            try {
                val result = recognizer.refine(getApplication(), imageUri, items, instruction)
                _state.update {
                    it.copy(isRefining = false, items = result.items, comment = result.comment)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(isRefining = false, error = error.message ?: "AI 修正失败，请稍后重试")
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun reportError(message: String) = _state.update { it.copy(error = message) }

    fun clear() {
        recognitionJob?.cancel()
        savedStateHandle[KEY_IN_PROGRESS] = false
        _state.value = RecognitionUiState()
    }

    private companion object {
        const val KEY_IMAGE = "recognition_image"
        const val KEY_IN_PROGRESS = "recognition_in_progress"
        val WAIT_LINES = listOf(
            "正在仔细看看这盘菜是什么…",
            "让我猜猜这顿吃了多少卡路里 🔍",
            "正在努力辨认食材，马上就好~",
            "AI 小助手正在认真计算中…",
            "先别急，我正在看看酱汁和配菜 👀",
            "把每一口都认真估一估，很快就有结果啦",
            "这盘看起来不错，我正在拆解食材",
            "正在帮你整理这顿饭的营养小账本 📒"
        )
    }
}
