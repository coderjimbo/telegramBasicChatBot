package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

@Data
public class MessageSendRequest {
    private Long chat_id;
    private String text;
    private Long reply_to_message_id;
}
