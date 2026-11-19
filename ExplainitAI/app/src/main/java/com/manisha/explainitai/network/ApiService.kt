package com.manisha.explainitai.network

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

data class ExtractResponse(
    val totalPages: Int,
    val pageNumber: Int,
    val pageText: String
)

data class ExplainResponse(val explanation: String)

data class MCQ(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)

data class MCQResponse(val mcqs: List<MCQ>)

// Added ChatResponse model for the Chat with Book feature
data class ChatResponse(val answer: String)

interface ApiService {
    @Multipart
    @POST("api/extract-page")
    suspend fun extractPage(
        @Part pdf: MultipartBody.Part,
        @Part("pageNumber") pageNumber: RequestBody
    ): Response<ExtractResponse>

    @POST("api/explain")
    suspend fun explainText(@Body body: Map<String, String>): Response<ExplainResponse>

    @POST("api/chat-page")
    suspend fun chatWithPage(@Body body: Map<String, String>): Response<ChatResponse>

    // Single unified method supporting both text strings and integer question counts
    @POST("api/generate-mcq")
    suspend fun generateMcqs(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<MCQResponse>

    companion object {
        private const val BASE_URL = "http://192.168.29.111:3000/"




        // Custom OkHttpClient with extended timeouts for AI generation (180 seconds)
        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val instance: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}