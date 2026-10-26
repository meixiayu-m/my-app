package com.calorielens.app.recognition

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal enum class RequestStage(val label: String) {
    CONNECTING("建立连接"),
    DNS("解析域名"),
    TLS("建立安全连接"),
    UPLOADING("上传图片"),
    WAITING("等待模型响应"),
    READING("接收响应")
}

internal object RecognitionNetworkPolicy {
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 90L
    const val CALL_TIMEOUT_SECONDS = 120L
    const val RETRY_DELAY_MS = 2500L
    const val MAX_RETRIES = 2

    fun shouldRetry(statusCode: Int, retries: Int): Boolean =
        statusCode == 429 && retries < MAX_RETRIES

    // 不展示异常原文、请求头或图片内容，诊断信息可以安全截图。
    fun failureMessage(error: IOException, stage: RequestStage, elapsedMs: Long): String {
        val prefix = "${stage.label}阶段失败（约 ${elapsedMs.coerceAtLeast(0) / 1000} 秒）"
        val reason = when (error) {
            is UnknownHostException -> "无法解析 open.bigmodel.cn，请检查手机网络或 DNS。"
            is SSLException -> "HTTPS 安全连接失败，请检查系统时间、证书或代理；不要关闭证书校验。"
            is SocketTimeoutException, is InterruptedIOException -> when (stage) {
                RequestStage.UPLOADING -> "图片上传超时，可切换 Wi-Fi/移动网络再试。"
                RequestStage.WAITING -> "等待智谱模型响应超时，可能是服务排队或网络延迟，尚未收到 HTTP 状态码。"
                RequestStage.READING -> "接收智谱响应超时，网络中断或服务返回较慢。"
                else -> "连接超时，请检查手机网络、DNS 或代理。"
            }
            is ConnectException -> "无法连接智谱，请检查手机网络或代理。"
            else -> "网络连接中断，请稍后重试。"
        }
        return "$prefix：$reason"
    }
}
