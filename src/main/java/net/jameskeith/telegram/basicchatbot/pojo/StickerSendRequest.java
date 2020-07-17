package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

@Data
public class StickerSendRequest {
    private Long chat_id;
    private String sticker;
}
