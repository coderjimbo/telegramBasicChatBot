package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

@Data
public class Update {
    public Integer update_id;
    public Message message;
    public Message edited_message;
    public Message channel_post;
    public Message edited_channel_post;
}
