package net.jameskeith.telegram.basicchatbot;

import net.jameskeith.telegram.basicchatbot.pojo.*;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class LongPollJob implements Job {
    @Value("${telegram.bot.name}")
    private String botUsername;

    @Value("${job.frequency}")
    private Integer jobFrequency;

    @Value("${telegram.api.url.get.updates}")
    private String getUpdatesUrl;

    @Value("${telegram.api.url.send.message}")
    private String sendMessageUrl;

    @Value("${telegram.response.list}")
    private String[] botResponses;

    @Value("${telegram.api.url.send.sticker}")
    private String sendStickerUrl;

    @Value("${telegram.sticker.list}")
    private String[] stickerList;

    @Value("${telegram.api.url.send.venue}")
    private String sendVenueUrl;

    @Value("${telegram.venue.pre.text}")
    private String venuePreText;

    @Value("${telegram.venue.address}")
    private String venueAddress;

    @Value("${telegram.venue.title}")
    private String venueTitle;

    @Value("${telegram.venue.latitude}")
    private Float venueLatitude;

    @Value("${telegram.venue.longitude}")
    private Float venueLongitude;

    @Value("${telegram.trigger.keywords}")
    private String[] triggerKeywords;

    @Autowired
    private RestTemplate restTemplate;

    private Integer lastUpdateOffset = 0;

    private Random random = new Random();

    public LongPollJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
        lastUpdateOffset = map.getInt("lastUpdateOffset");

        String requestUrl = getUpdatesUrl + "?offset=" + lastUpdateOffset
                + "&timeout=" + (jobFrequency / 1000);
        System.out.println("Polling for updates at " + requestUrl); // Each message sent to the bot is considered as an "update"
        TelegramApiListResponse<Update> updatesToProcess = fetchTelegramUpdates(requestUrl);

        for(Update update : updatesToProcess.getResult()) {
            handleUpdate(update);
            lastUpdateOffset = update.getUpdate_id();
        }

        if(updatesToProcess.getResult().size() != 0) {
            lastUpdateOffset += 1; // This ensures our next check for updates will disregard all updates with a lesser offset
        }
        map.put("lastUpdateOffset", lastUpdateOffset);
    }

    private TelegramApiListResponse<Update> fetchTelegramUpdates(String requestUrl) throws JobExecutionException {
        TelegramApiListResponse<Update> updatesToProcess = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<TelegramApiListResponse<Update>>() {}
        ).getBody();

        if(updatesToProcess.getOk()) {
            if(updatesToProcess.getResult().size() > 0) {
                System.out.println(updatesToProcess.getResult().size() + " updates fetched OK");
            }
        } else {
            throw new JobExecutionException("Failed to fetch updates from Telegram - error: " + updatesToProcess.getDescription());
        }
        return updatesToProcess;
    }

    private void handleUpdate(Update update) {
        if(updateContainsMessageEntities(update)) {
            handleMessageEntities(update); // This method checks for "entities" - mainly @ mentions so the bot can directly respond
        } else if(messageIsDirectToBot(update)) {
            respondToMessage(update); // This method handles DMs to the bot
        } else if (messageContainsTriggerKeyword(update.getMessage())) {
            respondToMessage(update); // This method responds to trigger keywords during normal group conversation
        }
    }

    private boolean updateContainsMessageEntities(Update update) {
        return update.getMessage() != null
                && update.getMessage().getText() != null
                && update.getMessage().getEntities() != null
                && update.getMessage().getEntities().size() > 0;
    }

    private void handleMessageEntities(Update update) {
        for(MessageEntity e : update.getMessage().getEntities()) {
            if(botUsernameMentioned(update, e)) {
                respondToMessage(update);
            }
        }
    }

    private boolean botUsernameMentioned(Update update, MessageEntity e) {
        return e.getType().equalsIgnoreCase("mention") && update.getMessage().getText().contains(botUsername);
    }

    private boolean messageIsDirectToBot(Update update) {
        return update.getMessage() != null && update.getMessage().getText() != null && update.getMessage().getChat().getType().equalsIgnoreCase("private");
    }

    private boolean messageContainsTriggerKeyword(Message message) {
        if(message != null && message.getText() != null) {
            String text = message.getText().toLowerCase();

            List<String> triggerKeywordsList = Arrays.asList(triggerKeywords);
            return triggerKeywordsList.contains(text);
        }
        return false;
    }

    private void respondToMessage(Update update) {
        String response = getRandomResponseString();

        if(!response.equalsIgnoreCase("[sticker]") && !response.equalsIgnoreCase("[venue]")) {
            sendMessageText(update, response); // If neither a sticker nor location is the random option chosen to send
        } else if (!response.equalsIgnoreCase("[venue]")) {
            sendStickerResponse(update); // If a location is not chosen to be sent
        } else {
            sendVenueResponse(update); // A location is sent
        }
    }

    private String getRandomResponseString() {
        return botResponses[random.nextInt(botResponses.length)];
    }

    private void sendMessageText(Update update, String textToSend) {
        MessageSendRequest request = createMessageSendRequest(update, textToSend);

        HttpEntity<MessageSendRequest> requestHttpEntity = new HttpEntity<>(request);
        TelegramApiResponse<Message> responseMessage = restTemplate.exchange(
                sendMessageUrl,
                HttpMethod.POST,
                requestHttpEntity,
                new ParameterizedTypeReference<TelegramApiResponse<Message>>() {}
        ).getBody();

        System.out.println("Text response sent: " + responseMessage.getOk());
    }

    private MessageSendRequest createMessageSendRequest(Update update, String textToSend) {
        MessageSendRequest request = new MessageSendRequest();
        request.setText(textToSend);
        request.setChat_id(update.getMessage().getChat().getId());
        return request;
    }

    private void sendStickerResponse(Update update) {
        StickerSendRequest request = createStickerSendRequest(update);

        HttpEntity<StickerSendRequest> requestHttpEntity = new HttpEntity<>(request);
        TelegramApiResponse<Message> responseMessage = restTemplate.exchange(
                sendStickerUrl,
                HttpMethod.POST,
                requestHttpEntity,
                new ParameterizedTypeReference<TelegramApiResponse<Message>>() {}
        ).getBody();

        System.out.println("Sticker response sent: " + responseMessage.getOk());
    }

    private StickerSendRequest createStickerSendRequest(Update update) {
        StickerSendRequest request = new StickerSendRequest();
        request.setSticker(getRandomStickerResponse());
        request.setChat_id(update.getMessage().getChat().getId());
        return request;
    }

    private String getRandomStickerResponse() {
        return stickerList[random.nextInt(stickerList.length)];
    }

    private void sendVenueResponse(Update update) {
        sendMessageText(update, venuePreText);

        VenueSendRequest request = createVenueSendRequest(update);
        HttpEntity<VenueSendRequest> requestHttpEntity = new HttpEntity<>(request);

        TelegramApiResponse<Message> responseMessage = restTemplate.exchange(
                sendVenueUrl,
                HttpMethod.POST,
                requestHttpEntity,
                new ParameterizedTypeReference<TelegramApiResponse<Message>>() {}
        ).getBody();

        System.out.println("Venue response sent: " + responseMessage.getOk());
    }

    private VenueSendRequest createVenueSendRequest(Update update) {
        VenueSendRequest request = new VenueSendRequest();
        request.setAddress(venueAddress);
        request.setTitle(venueTitle);
        request.setLatitude(venueLatitude);
        request.setLongitude(venueLongitude);
        request.setChat_id(update.getMessage().getChat().getId());
        return request;
    }
}
