import re

with open('app/src/main/java/com/example/GeminiApiClient.kt', 'r') as f:
    content = f.read()

# Add a StateFlow for GeminiStatus
flow_imports = "import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow\n"
content = content.replace("package com.example\n", "package com.example\n\n" + flow_imports)

status_flow = """    private val _status = MutableStateFlow(GeminiStatus.READY)
    val status: StateFlow<GeminiStatus> = _status.asStateFlow()

    private fun updateStatus(newStatus: GeminiStatus) {
        _status.value = newStatus
    }

"""

content = content.replace("private val jsonMediaType =", status_flow + "    private val jsonMediaType =")

# Add updateStatus calls
content = content.replace("GeminiResult.MissingApiKey", "{ updateStatus(GeminiStatus.MISSING_KEY); GeminiResult.MissingApiKey }()")
content = content.replace("GeminiResult.ApiError(400", "{ updateStatus(GeminiStatus.SERVER_ERROR); GeminiResult.ApiError(400")
content = content.replace("GeminiResult.ApiError(response.code, \"Authentication/Permission Error\")", "{ updateStatus(GeminiStatus.AUTH_ERROR); GeminiResult.ApiError(response.code, \"Authentication/Permission Error\") }()")
content = content.replace("GeminiResult.ApiError(404", "{ updateStatus(GeminiStatus.MODEL_ERROR); GeminiResult.ApiError(404")
content = content.replace("GeminiResult.ApiError(429", "{ updateStatus(GeminiStatus.QUOTA_EXCEEDED); GeminiResult.ApiError(429")
content = content.replace("GeminiResult.ApiError(response.code, \"Server Error: $responseBody\")", "{ updateStatus(GeminiStatus.SERVER_ERROR); GeminiResult.ApiError(response.code, \"Server Error: $responseBody\") }()")
content = content.replace("GeminiResult.ApiError(response.code, \"Unknown API Error: $responseBody\")", "{ updateStatus(GeminiStatus.UNKNOWN_ERROR); GeminiResult.ApiError(response.code, \"Unknown API Error: $responseBody\") }()")
content = content.replace("GeminiResult.ParseError(\"Empty response body\")", "{ updateStatus(GeminiStatus.PARSE_ERROR); GeminiResult.ParseError(\"Empty response body\") }()")
content = content.replace("GeminiResult.ParseError(\"No candidates returned", "{ updateStatus(GeminiStatus.PARSE_ERROR); GeminiResult.ParseError(\"No candidates returned")
content = content.replace("GeminiResult.ParseError(\"Missing text in response", "{ updateStatus(GeminiStatus.PARSE_ERROR); GeminiResult.ParseError(\"Missing text in response")
content = content.replace("GeminiResult.Success(text)", "{ updateStatus(GeminiStatus.READY); GeminiResult.Success(text) }()")
content = content.replace("GeminiResult.ParseError(\"Malformed JSON", "{ updateStatus(GeminiStatus.PARSE_ERROR); GeminiResult.ParseError(\"Malformed JSON")
content = content.replace("GeminiResult.NetworkError(\"Connection timed out", "{ updateStatus(GeminiStatus.NETWORK_ERROR); GeminiResult.NetworkError(\"Connection timed out")
content = content.replace("GeminiResult.NetworkError(\"Network disconnected or host unreachable", "{ updateStatus(GeminiStatus.NETWORK_ERROR); GeminiResult.NetworkError(\"Network disconnected or host unreachable")
content = content.replace("GeminiResult.NetworkError(\"Network error", "{ updateStatus(GeminiStatus.NETWORK_ERROR); GeminiResult.NetworkError(\"Network error")
content = content.replace("GeminiResult.ApiError(-1", "{ updateStatus(GeminiStatus.UNKNOWN_ERROR); GeminiResult.ApiError(-1")

with open('app/src/main/java/com/example/GeminiApiClient.kt', 'w') as f:
    f.write(content)
