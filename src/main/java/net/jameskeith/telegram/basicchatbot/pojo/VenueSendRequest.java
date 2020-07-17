package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

@Data
public class VenueSendRequest {
    private Long chat_id;
    private Float latitude;
    private Float longitude;
    private String title;
    private String address;
}
