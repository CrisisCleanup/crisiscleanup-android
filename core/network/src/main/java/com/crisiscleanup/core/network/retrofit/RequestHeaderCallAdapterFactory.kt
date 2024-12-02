package com.crisiscleanup.core.network.retrofit

import okhttp3.Request
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type

enum class RequestHeaderKey {
    AccessTokenAuth,
    WrapResponse,
    ThrowClientError,
}

/**
 * IDs for endpoints with the exact same method and URL but must be treated as distinct endpoints
 *
 * For retrieving cached data.
 * Primarily for endpoints serving different data when authenticated vs not authenticated.
 */
enum class EndpointRequestId {
    MyProfile,
    MyProfileNoAuth,
    Incidents,
    IncidentsNoAuth,
}

class RequestHeaderKeysLookup(
    /**
     * Generic lookup for requests with distinct request method+URL
     */
    private val lookup: MutableMap<Int, Map<RequestHeaderKey, String>> = mutableMapOf(),
    /**
     * Lookup for requests with same request method+URL but different headers
     */
    private val requestIdLookup: MutableMap<EndpointRequestId, Map<RequestHeaderKey, String>> = mutableMapOf(),
) {
    private fun requestKey(request: Request) =
        request.url.hashCode() + 31 * request.method.hashCode()

    fun getHeaderKeys(request: Request): Map<RequestHeaderKey, String>? {
        return request.tag(EndpointRequestId::class.java)?.let {
            requestIdLookup[it]
        } ?: lookup[requestKey(request)]
    }

    internal fun setHeaderKeys(request: Request, annotations: Array<out Annotation>) {
        var requestKey = 0
        val requestId = request.tag(EndpointRequestId::class.java)
        if (requestId == null) {
            requestKey = requestKey(request)
            if (lookup.containsKey(requestKey)) {
                return
            }
        } else if (requestIdLookup.containsKey(requestId)) {
            return
        }

        val requestKeys = mutableMapOf<RequestHeaderKey, String>()
        if (requestId == null) {
            lookup[requestKey] = requestKeys
        } else {
            requestIdLookup[requestId] = requestKeys
        }

        annotations.forEach {
            when (it.annotationClass) {
                TokenAuthenticationHeader::class ->
                    requestKeys[RequestHeaderKey.AccessTokenAuth] = ""

                WrapResponseHeader::class ->
                    requestKeys[RequestHeaderKey.WrapResponse] = (it as WrapResponseHeader).key

                ThrowClientErrorHeader::class ->
                    requestKeys[RequestHeaderKey.ThrowClientError] = ""
            }
        }
    }
}

internal class HeaderKeysCallAdapter<T : Any>(
    private val adapter: CallAdapter<Any, T>,
    private val annotations: Array<out Annotation>,
    private val headerKeysLookup: RequestHeaderKeysLookup,
) : CallAdapter<Any, T> {
    override fun responseType(): Type = adapter.responseType()

    override fun adapt(call: Call<Any>): T {
        headerKeysLookup.setHeaderKeys(call.request(), annotations)
        return adapter.adapt(call)
    }
}

// Based off of https://www.andretietz.com/2020/10/09/custom-retrofit2-annotations/#Defining-the-annotation
internal class RequestHeaderCallAdapterFactory(
    private val headerKeysLookup: RequestHeaderKeysLookup,
) : CallAdapter.Factory() {
    @Suppress("UNCHECKED_CAST")
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        val matchingAdapterFactories =
            retrofit.callAdapterFactories().filterNot { it is RequestHeaderCallAdapterFactory }
        for (adapterFactory in matchingAdapterFactories) {
            adapterFactory.get(returnType, annotations, retrofit)?.let {
                return if (annotations.isEmpty()) {
                    it
                } else {
                    HeaderKeysCallAdapter(
                        it as CallAdapter<Any, Any>,
                        annotations,
                        headerKeysLookup,
                    )
                }
            }
        }
        return null
    }
}
