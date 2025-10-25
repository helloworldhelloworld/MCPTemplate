package com.example.mcp.client.demo;

import com.example.mcp.client.McpClient;
import com.example.mcp.client.api.TranslationApi;
import com.example.mcp.client.api.TranslationApi.TranslationResponse;
import com.example.mcp.client.interceptor.HmacAuthInterceptor;
import com.example.mcp.client.interceptor.TraceInterceptor;
import com.example.mcp.client.transport.HttpTransport;
import com.example.mcp.common.StdResponse;
import java.util.Arrays;
import okhttp3.Interceptor;

public class ClientDemo {
  public static void main(String[] args) throws Exception {
    Interceptor hmac = new HmacAuthInterceptor("demo-client", "local-secret");
    Interceptor trace = new TraceInterceptor();
    HttpTransport transport = new HttpTransport("http://localhost:8080", Arrays.asList(trace, hmac));
    McpClient client = new McpClient("demo-client", transport);
    TranslationApi api = new TranslationApi(client);
    StdResponse<TranslationResponse> response =
        api.translate("hello world", "en", "es");
    System.out.println("Status: " + response.getStatus());
    if (response.getData() != null) {
      System.out.println("Translation: " + response.getData().getTranslatedText());
    }
  }
}
