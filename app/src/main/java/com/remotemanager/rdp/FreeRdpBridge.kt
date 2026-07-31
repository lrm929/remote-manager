package com.remotemanager.rdp

/**
 * FreeRDP 原生桥接占位。
 *
 * 完整内置 RDP 客户端需要集成 FreeRDP 原生库（libfreerdp2 / libfreerdp-client2 /
 * libwinpr2 等）并通过 JNI 暴露连接、断开、发送输入、接收帧回调等接口。
 *
 * 后续步骤：
 * 1. 在 app/src/main/cpp 下添加 FreeRDP CMake 子模块或预编译 jniLibs。
 * 2. 配置 build.gradle.kts 的 externalNativeBuild。
 * 3. 实现 native 方法：connect / disconnect / sendInput / setSurface 等。
 * 4. 在 RdpSessionScreen 中用 SurfaceView/TextureView 渲染远端帧。
 */
object FreeRdpBridge {

    /**
     * 检查当前 APK 是否已打包 FreeRDP 原生库。
     */
    fun isAvailable(): Boolean {
        // TODO: 加载 libfreerdp-android2.so 并验证 native 方法。
        return false
    }

    /**
     * 尝试使用 FreeRDP 原生客户端连接 RDP 服务器。
     * 当前未实现，返回失败。
     */
    fun connect(
        host: String,
        port: Int,
        username: String,
        password: String?,
        domain: String?,
        width: Int,
        height: Int,
        colorDepth: Int,
        useNla: Boolean
    ): Result<Unit> {
        return Result.failure(NotImplementedError("FreeRDP native 库尚未集成"))
    }

    fun disconnect() {
        // TODO
    }
}
