package com.example.movieapp.core.network

/**
 * Centralized network configuration and Base URL constants.
 * TODO: Replace placeholders with final production / staging URLs after CONFIG-GATE sign-off.
 */
object NetworkConfig {
    /**
     * Default Base URL for Android Emulator pointing to local host Gateway.
     * Note: 127.0.0.1 refers to the emulator itself.
     * Use 10.0.2.2 to access host machine port 3000, or run `adb reverse tcp:3000 tcp:3000`.
     */
    const val DEV_EMULATOR_BASE_URL = "http://10.0.2.2:3000/"
    
    /**
     * Base URL when using adb reverse or local loopback.
     */
    const val DEV_LOCALHOST_BASE_URL = "http://127.0.0.1:3000/"
    
    /**
     * Placeholder for staging environment (HTTPS required).
     */
    const val STAGING_BASE_URL = "https://movie.tft5s.com/"
    
    /**
     * Placeholder for production environment (HTTPS required).
     */
    const val PROD_BASE_URL = "https://movie.tft5s.com/"

    /**
     * Active Base URL used by Retrofit clients.
     */
    val activeBaseUrl: String
        get() = PROD_BASE_URL
}
