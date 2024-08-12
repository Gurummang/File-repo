package com.GASB.file.model.dto.response.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDto<T> {
    private String status;
    private Long file_id;
    private String message;
    private T data;

    public static <T> ResponseDto<T> ofSuccess() {
        return ResponseDto.<T>builder()
                .status("success")
                .build();
    }

    public static <T> ResponseDto<T> ofSuccess(Long file_id, T data) {
        return ResponseDto.<T>builder()
                .status("success")
                .file_id(file_id)
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> ofSuccess(T data) {
        return ResponseDto.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    public static <T> ResponseDto<T> ofFail(String message) {
        return ResponseDto.<T>builder()
                .status("error")
                .message(message)
                .build();
    }

    public static <T> ResponseDto<T> ofFail(Long file_id, String message) {
        return ResponseDto.<T>builder()
                .status("error")
                .file_id(file_id)
                .message(message)
                .build();
    }
}
