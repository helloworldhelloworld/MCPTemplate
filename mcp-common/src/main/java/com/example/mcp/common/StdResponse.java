package com.example.mcp.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StdResponse<T> implements Serializable {
  @JsonProperty("status")
  private String status;

  @JsonProperty("code")
  private String code;

  @JsonProperty("message")
  private String message;

  @JsonProperty("data")
  private T data;

  public static <T> StdResponse<T> success(String code, String message, T data) {
    StdResponse<T> response = new StdResponse<>();
    response.setStatus("success");
    response.setCode(code);
    response.setMessage(message);
    response.setData(data);
    return response;
  }

  public static <T> StdResponse<T> error(String code, String message) {
    StdResponse<T> response = new StdResponse<>();
    response.setStatus("error");
    response.setCode(code);
    response.setMessage(message);
    return response;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }
}
