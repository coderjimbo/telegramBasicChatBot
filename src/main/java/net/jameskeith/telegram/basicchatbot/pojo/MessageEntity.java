package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

@Data
public class MessageEntity {
    private String type;
    private String url;
    private User user;
}
