package com.example.movieapp.domain.usecase

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PersonalizedHome
import com.example.movieapp.domain.repository.CatalogRepository
import javax.inject.Inject

class GetPublicHomeUseCase @Inject constructor(
    private val catalogRepository: CatalogRepository
) {
    suspend operator fun invoke(pageSize: Int = 10): Result<CatalogHome> {
        return catalogRepository.getPublicHome(pageSize)
    }
}

class GetPersonalizedHomeUseCase @Inject constructor(
    private val catalogRepository: CatalogRepository
) {
    suspend operator fun invoke(profileId: String): Result<PersonalizedHome> {
        return catalogRepository.getPersonalizedHome(profileId)
    }
}

class BrowseMoviesUseCase @Inject constructor(
    private val catalogRepository: CatalogRepository
) {
    suspend operator fun invoke(
        page: Int? = null,
        pageSize: Int? = null,
        q: String? = null,
        genre: String? = null,
        country: String? = null,
        year: Int? = null,
        type: String? = null,
        contentKind: String? = null,
        sourceType: String? = null,
        provider: String? = null,
        sort: String? = null,
        profileId: String? = null
    ): Result<List<Movie>> {
        return catalogRepository.getMovies(
            page = page,
            pageSize = pageSize,
            q = q,
            genre = genre,
            country = country,
            year = year,
            type = type,
            contentKind = contentKind,
            sourceType = sourceType,
            provider = provider,
            sort = sort,
            profileId = profileId
        )
    }
}

class SearchMoviesUseCase @Inject constructor(
    private val catalogRepository: CatalogRepository
) {
    suspend operator fun invoke(
        q: String,
        page: Int = 1,
        pageSize: Int = 20,
        profileId: String? = null
    ): Result<List<Movie>> {
        return catalogRepository.search(q, page, pageSize, profileId)
    }
}

class GetMovieDetailUseCase @Inject constructor(
    private val catalogRepository: CatalogRepository
) {
    suspend operator fun invoke(movieId: String, profileId: String? = null): Result<MovieDetail> {
        return catalogRepository.getMovieDetail(movieId, profileId)
    }
}
