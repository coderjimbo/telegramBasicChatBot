package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

import java.util.List;

@Data
public class TelegramApiListResponse<T> {
    private Boolean ok;
    private String description;
    private Integer error_code;
    private List<T> result;
}
