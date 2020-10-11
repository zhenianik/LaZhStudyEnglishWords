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
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class Bot extends TelegramLongPollingBot {

    private static Connection myCon = null;
    private static String currentWord = "";
    private static HashMap<String, String> properties = getProperties();

    public static void main(String[] args) throws SQLException {

        SetConnection(properties.get("url"), properties.get("username"), properties.get("password"));

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, String> getProperties() {
        HashMap<String, String> properties = new HashMap<>();

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
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        properties.put("url", props.getProperty("jdbc.url"));
        properties.put("username", props.getProperty("jdbc.username"));
        properties.put("password", props.getProperty("jdbc.password"));
        properties.put("telegramBotToken", props.getProperty("telegramBotToken"));
        properties.put("telegramBotUsername", props.getProperty("telegramBotUsername"));

        return properties;
    }

    private static void SetConnection(String url, String username, String password) {

        try {
            myCon = DriverManager.getConnection(url, username, password);
        } catch (SQLException throwables) {
            //System.out.println("don't work");
            throwables.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        ArrayList<String> buttonList = new ArrayList<>();
        buttonList.add("/test");
        buttonList.add("/video");

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();
            if ("/test".equals(text)) {
                ArrayList<String> answer = getRequest(getRandomWord());
                sendMsg(message, answer.toString(), true, buttonList);
            } else if ("/video".equals(text)) {
                ArrayList<String> answer = getRequest(getRandomVideo());
                for (String str : answer) {
                    String w[] = str.split(";");
                    sendMsg(message, w.toString(), false, buttonList);
                }
            } else if ("/yes".equals(text)) {
                boolean answer = getRequestInsert(addNewWord(currentWord, message.getFrom().getUserName()));
                if (answer) sendMsg(message, "Слово успешно добавлено", false, buttonList);
                else sendMsg(message, "Что-то пошло не так, слово не добавлено.", false, buttonList);
            } else if ("/no".equals(text)) {
                sendMsg(message, "Хорошо, не будем добавлять.", false, buttonList);
            } else {
                // если ввели слово (не команда), тогда проверим его наличие в бд и выведем соотв.результат
                ArrayList<String> answer = getRequest(chekWord(text));
                if (answer.size() != 0) {
                    for (String str : answer) {
                        String arr[] = str.split(";");
                        String mystr = "перевод: ";
                        for (int i = 1; i < arr.length - 1; i++) {
                            String s = arr[i + 1].equals("") ? "" : ", ";
                            if (!arr[i].equals("")) mystr = mystr + arr[i] + s;
                        }
                        sendMsg(message, mystr, true, buttonList);
                    }
                } else {
                    ArrayList<String> buttonListAdd = new ArrayList<>();
                    buttonListAdd.add("/yes");
                    buttonListAdd.add("/no");
                    sendMsg(message, "Такого слова нет в словаре! Добавить?", true, buttonListAdd);
                    currentWord = text;
                }

            }
        }
    }

    public void setButtons(SendMessage sendMessage, ArrayList<String> buttonList) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRowList = new ArrayList<KeyboardRow>();
        KeyboardRow keyboarfirstrow = new KeyboardRow();

        for (String s : buttonList) {
            keyboarfirstrow.add(new KeyboardButton(s));
        }

        keyboardRowList.add(keyboarfirstrow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
    }

    public void sendMsg(Message message, String text, boolean enableMarkdown, ArrayList<String> buttonList) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(enableMarkdown);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);

        try {
            setButtons(sendMessage, buttonList);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    public String getBotUsername() {
        return properties.get("telegramBotUsername");
    }

    public String getBotToken() {
        return properties.get("telegramBotToken");
    }

    public static ArrayList<String> getRequest(String sqlText) {
        ArrayList<String> result = new ArrayList<String>();
        Statement myStmt = null;
        try {
            if (myCon.isClosed()) SetConnection(properties.get("url"), properties.get("username"), properties.get("password"));
            myStmt = myCon.createStatement();
            ResultSet rs = myStmt.executeQuery(sqlText);
            while (rs.next()) {
                result.add(rs.getString("word") + ";" + rs.getString("translate1") +
                        ";" + rs.getString("translate2") + ";" + rs.getString("translate3") +
                        ";" + rs.getString("translate4") + ";" + rs.getString("context"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    public static boolean getRequestInsert(String sqlText) {
        Statement myStmt = null;
        try {
            myStmt = myCon.createStatement();
            myStmt.executeUpdate(sqlText);
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
    }

    public static String getRandomVideo() {
        return "SELECT DISTINCT word, translate1, translate2, translate3, translate4, context from words order by RAND() LIMIT 1";
    }

    public static String getRandomWord() {
        return "SELECT word, translate1, translate2, translate3, translate4, context from words order by RAND() LIMIT 10";
    }

    public static String chekWord(String text) {
        return "SELECT word, translate1, translate2, translate3, translate4, context from words where word = '" + text + "'";
    }

    public static String addNewWord(String text, String context) {
        return "INSERT INTO `words` (`id_word`, `word`, `translate1`, `translate2`, `translate3`, `translate4`, `context`) " +
                "VALUES (NULL, '" + text + "', '', '', '', '', '" + context + "')";
    }

}

