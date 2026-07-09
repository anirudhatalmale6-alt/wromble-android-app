package dk.wromble.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.wromble.app.data.Api
import dk.wromble.app.data.ChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

// Kundeservice-chat (matcher iOS ChatViewModel: chat-start / chat-send / chat-upload / chat-poll)
class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    var conversationId by mutableStateOf(0)
        private set
    var status by mutableStateOf("open")
        private set
    var isStarted by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isUploading by mutableStateOf(false)
        private set
    var senderName by mutableStateOf("")
        private set

    private val seen = HashSet<Int>()
    private var lastId = 0
    private var pollJob: Job? = null

    fun start(name: String, email: String) {
        if (isLoading) return
        isLoading = true
        senderName = name.ifBlank { "Kunde" }
        viewModelScope.launch {
            try {
                val r = Api.service.chatStart(mapOf("name" to senderName, "email" to email))
                val cid = r.conversationId ?: 0
                if (cid > 0) {
                    conversationId = cid
                    isStarted = true
                    startPolling()
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun send(text: String) {
        if (conversationId <= 0 || text.isBlank()) return
        viewModelScope.launch {
            try {
                Api.service.chatSend(
                    mapOf(
                        "conversation_id" to conversationId,
                        "sender_type" to "customer",
                        "sender_name" to senderName,
                        "message" to text
                    )
                )
                poll()
            } catch (_: Exception) {
            }
        }
    }

    fun upload(file: File, mime: String) {
        if (conversationId <= 0) return
        isUploading = true
        viewModelScope.launch {
            try {
                fun field(v: String) = v.toRequestBody("text/plain".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "file", file.name, file.asRequestBody(mime.toMediaTypeOrNull())
                )
                Api.service.chatUpload(
                    field(conversationId.toString()),
                    field("customer"),
                    field(senderName),
                    part
                )
                poll()
            } catch (_: Exception) {
            } finally {
                isUploading = false
                runCatching { file.delete() }
            }
        }
    }

    // Genoptag polling naar man kommer tilbage til chat-fanen
    fun resume() {
        if (isStarted && pollJob?.isActive != true) startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                poll()
                delay(3000)
            }
        }
    }

    private suspend fun poll() {
        if (conversationId <= 0) return
        try {
            val r = Api.service.chatPoll(conversationId, lastId)
            status = r.status
            for (m in r.messages) {
                if (m.id > 0 && seen.contains(m.id)) continue
                if (m.id > 0) seen.add(m.id)
                messages.add(m)
                if (m.id > lastId) lastId = m.id
            }
        } catch (_: Exception) {
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        stop()
    }
}
