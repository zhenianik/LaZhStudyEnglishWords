import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    public static void main(String[] args) throws SQLException {

        String url = "jdbc:mysql://mysql-srv30737.hts.ru/srv30737_english?useUnicode=true&serverTimezone=UTC";
        String user = "srv30737_english";
        String password = "74nik74";
        try {
            Connection myCon = DriverManager.getConnection(url, user, password);
            Statement myStmt = myCon.createStatement();
            String sql = "select * from srv30737_english.words";
            ResultSet rs = myStmt.executeQuery(sql);
            while (rs.next()) {
                System.out.println(rs.getString("word"));
            }
        } catch (SQLException throwables) {
            //System.out.println("don't work");
            throwables.printStackTrace();
        }

//        ApiContextInitializer.init();
//        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
//        try {
//            telegramBotsApi.registerBot(new Bot());
//        } catch (TelegramApiRequestException e) {
//            e.printStackTrace();
//        }
    }

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();
            if ("/help".equals(text)) {
                sendMsg(message, "Чем я могу помочь");
            } else if ("/setting".equals(text)) {
                sendMsg(message, "Что будем настраивать");
            } else {
                sendMsg(message, "А не пошёл бы ты в жопу?!");
            }
        }
    }

    public void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRowList = new ArrayList<KeyboardRow>();
        KeyboardRow keyboarfirstrow = new KeyboardRow();

        keyboarfirstrow.add(new KeyboardButton("/help"));
        keyboarfirstrow.add(new KeyboardButton("/setting"));

        keyboardRowList.add(keyboarfirstrow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
    }

    public void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);

        try {
            setButtons(sendMessage);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    public String getBotUsername() {
        return "LaZhStudyEnglishWords_bot";
    }

    public String getBotToken() {
        return "1175991949:AAFgsPgTp5yKCFV0J1grvL8E-mVNN4vYbWc";
    }
}
