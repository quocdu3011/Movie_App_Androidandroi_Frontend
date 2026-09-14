package com.example.movieapp.core.network

import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedCookieJarProvider @Inject constructor() {
    val cookieManager: CookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    }

    val cookieJar: CookieJar = JavaNetCookieJar(cookieManager)
}
