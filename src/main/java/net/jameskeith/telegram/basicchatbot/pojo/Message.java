package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

import java.util.List;

@Data
public class Message {
    public Integer message_id;
    public User from;
    public Integer date;
    public Chat chat;
    public User forward_from;
    public User forward_from_chat;
    public Integer forward_from_message_id;
    public String forward_signature;
    public String forward_sender_name;
    public Integer forward_date;
    public Message reply_to_message;
    public User via_bot;
    public Integer edit_date;
    public String media_group_id;
    public String author_signature;
    public String text;
    public List<MessageEntity> entities;
    public Sticker sticker;
}
