package macos

import okhttp3.HttpUrl

data class MacOSClient(val host: String, val port: Int) {
    fun sidecarAPIBuilder(pathSegment: String): HttpUrl.Builder {
        return HttpUrl.Builder()
            .scheme("http")
            .host(host)
            .addPathSegment(pathSegment)
            .port(port)
    }
}
