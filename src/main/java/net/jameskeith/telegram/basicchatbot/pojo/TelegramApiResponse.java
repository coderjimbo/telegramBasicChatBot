package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

@Data
public class TelegramApiResponse<T> {
    private Boolean ok;
    private String description;
    private Integer error_code;
    private T result;
}
