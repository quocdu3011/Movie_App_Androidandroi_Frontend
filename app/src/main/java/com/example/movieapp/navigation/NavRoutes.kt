package com.example.movieapp.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PUBLIC_HOME = "public_home"
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val WATCH_HISTORY = "watch_history"
    const val PROFILE_LIST = "profile_list"
    const val PROFILE_CREATE = "profile_create"
    const val PROFILE_EDIT = "profile_edit/{profileId}"
    const val MOVIE_DETAIL = "movie_detail/{movieId}"
    const val PLAYER = "player/{movieId}/{playableId}/{sourceItemId}/{profileId}"
    const val PLANS = "plans"
    const val SETTINGS = "settings"

    fun profileEdit(profileId: String) = "profile_edit/$profileId"
    fun movieDetail(movieId: String) = "movie_detail/$movieId"
    fun player(movieId: String, playableId: String, sourceItemId: String, profileId: String) =
        "player/$movieId/$playableId/$sourceItemId/$profileId"
}
