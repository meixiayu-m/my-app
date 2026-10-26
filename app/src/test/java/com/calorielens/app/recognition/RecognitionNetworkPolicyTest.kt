package com.calorielens.app.recognition

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.*
import org.junit.Test

class RecognitionNetworkPolicyTest {
    @Test fun onlyRetries429Twice() {
        assertTrue(RecognitionNetworkPolicy.shouldRetry(429, 0))
        assertTrue(RecognitionNetworkPolicy.shouldRetry(429, 1))
        assertFalse(RecognitionNetworkPolicy.shouldRetry(429, 2))
        listOf(200, 400, 401, 403, 500, 503).forEach {
            assertFalse(RecognitionNetworkPolicy.shouldRetry(it, 0))
        }
        assertEquals(2500L, RecognitionNetworkPolicy.RETRY_DELAY_MS)
    }

    @Test fun timeoutSettingsDoNotUseOkHttpTenSecondDefault() {
        assertEquals(15L, RecognitionNetworkPolicy.CONNECT_TIMEOUT_SECONDS)
        assertEquals(30L, RecognitionNetworkPolicy.WRITE_TIMEOUT_SECONDS)
        assertEquals(90L, RecognitionNetworkPolicy.READ_TIMEOUT_SECONDS)
        assertEquals(120L, RecognitionNetworkPolicy.CALL_TIMEOUT_SECONDS)
    }

    @Test fun waitingTimeoutDoesNotClaimDnsFailure() {
        val message = RecognitionNetworkPolicy.failureMessage(SocketTimeoutException("timeout"), RequestStage.WAITING, 90001)
        assertTrue(message.contains("等待模型响应"))
        assertTrue(message.contains("90 秒"))
        assertTrue(message.contains("尚未收到 HTTP 状态码"))
        assertFalse(message.contains("DNS"))
    }

    @Test fun uploadAndBodyReadTimeoutAreDistinguishable() {
        val error = SocketTimeoutException("timeout")
        assertTrue(RecognitionNetworkPolicy.failureMessage(error, RequestStage.UPLOADING, 30000).contains("图片上传超时"))
        assertTrue(RecognitionNetworkPolicy.failureMessage(error, RequestStage.READING, 90000).contains("接收智谱响应超时"))
    }

    @Test fun totalCallTimeoutKeepsItsStage() {
        val message = RecognitionNetworkPolicy.failureMessage(InterruptedIOException("timeout"), RequestStage.WAITING, 120000)
        assertTrue(message.contains("等待模型响应"))
        assertTrue(message.contains("120 秒"))
    }

    @Test fun dnsTlsAndConnectFailuresAreDistinct() {
        assertTrue(RecognitionNetworkPolicy.failureMessage(UnknownHostException(), RequestStage.DNS, 0).contains("无法解析"))
        assertTrue(RecognitionNetworkPolicy.failureMessage(SSLHandshakeException("certificate"), RequestStage.TLS, 0).contains("证书"))
        assertTrue(RecognitionNetworkPolicy.failureMessage(ConnectException(), RequestStage.CONNECTING, 0).contains("无法连接"))
    }

    @Test fun diagnosticDoesNotEchoExceptionSecrets() {
        val message = RecognitionNetworkPolicy.failureMessage(IOException("Bearer fake-private-key"), RequestStage.READING, 1000)
        assertFalse(message.contains("fake-private-key"))
        assertFalse(message.contains("Bearer"))
    }
}
