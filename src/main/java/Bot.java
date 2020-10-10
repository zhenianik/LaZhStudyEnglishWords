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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Bot extends TelegramLongPollingBot {

    private static Connection myCon = null;
    private boolean connectionStatus = SetConnection();

    public static void main(String[] args) throws SQLException {

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    private boolean SetConnection() {
        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream("C:/java/db.properties");
            props.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String driver = props.getProperty("jdbc.driver");
        if (driver != null) {
            try {
                Class.forName(driver) ;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        String url = props.getProperty("jdbc.url");
        String username = props.getProperty("jdbc.username");
        String password = props.getProperty("jdbc.password");

        try {
            myCon = DriverManager.getConnection(url, username, password);
            return true;
        } catch (SQLException throwables) {
            //System.out.println("don't work");
            throwables.printStackTrace();
            return false;
        }
    }

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();
            if ("/help".equals(text)) {
                sendMsg(message, "Чем я могу помочь", true);
            } else if ("/setting".equals(text)) {
                sendMsg(message, "Что будем настраивать", true);
            } else if ("/video".equals(text)) {
                sendMsg(message, "https://youtu.be/m-5RSSZg0Os", false);
            } else {
                sendMsg(message, "А не пошёл бы ты в жопу?!", true);
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
        keyboarfirstrow.add(new KeyboardButton("/video"));

        keyboardRowList.add(keyboarfirstrow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
    }

    public void sendMsg(Message message, String text, boolean enableMarkdown) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(enableMarkdown);
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

    public String getRequest() {
        Statement myStmt = null;
        try {
            myStmt = myCon.createStatement();
            String sql = "select * from srv30737_english.words";
            ResultSet rs = myStmt.executeQuery(sql);
            while (rs.next()) {
                System.out.println(rs.getString("word"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return "";
    }
}
