package net.jameskeith.telegram.basicchatbot.pojo;

import lombok.Data;

@Data
public class User {
    public Integer id;
    public Boolean is_bot;
    public String first_name;
    public String last_name;
    public String username;
}
