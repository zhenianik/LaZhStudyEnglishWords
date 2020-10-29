package com.evgen.lazhstudyenglishwords;

import java.io.InputStream;
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

import java.io.IOException;
import java.sql.*;
import java.util.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

public class Bot extends TelegramLongPollingBot {

    private static Connection myCon = null;
    private static String currentWord = "";
    private static String currentTranslate = "";

    private static Properties properties = getProperties();

    public static void main(String[] args) {

        System.out.println("Ты пидор.");
        setConnection(properties.getProperty("jdbc.url"), properties.getProperty("jdbc.username"), properties.getProperty("jdbc.password"));

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new com.evgen.lazhstudyenglishwords.Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    private static Properties getProperties() {

        Properties props = new Properties();
        try (InputStream input = Bot.class.getClassLoader().getResourceAsStream("application.properties")) {
            props.load(input);
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

        return props;
    }

    private static void setConnection(String url, String username, String password) {

        try {
            myCon = DriverManager.getConnection(url, username, password);
        } catch (SQLException throwables) {
            //System.out.println("don't work");
            throwables.printStackTrace();
        }
    }

    public static String getTranslatedString(String arr[]) {
        String mystr = "";
        if (arr.length != 0) {
            mystr = mystr + arr[0] + " - ";
            for (int i = 1; i < arr.length - 1; i++) {
                String s = arr[i + 1].equals("") ? "" : ", ";
                if (!arr[i].equals("")) mystr = mystr + arr[i] + s;
            }
        }
        return mystr;
    }

    public String getResultStr(ArrayList<String> answer, boolean showContext) {
        String resultStr = "";
        if (answer.size() != 0) {
            for (String str : answer) {
                String arr[] = str.split(";");
                resultStr = resultStr + getTranslatedString(arr) + System.lineSeparator();
            }
            if (showContext) {
                String arr[] = answer.get(answer.size() - 1).split(";");
                resultStr = resultStr + System.lineSeparator() + arr[arr.length - 1];
            }
        }
        return resultStr;
    }

    public String addNewWordResult(String username, boolean add) {
        String resultStr = "";
        if (add) {
            boolean answer = getRequestInsert(addNewWord(currentWord, currentTranslate, username));
            if (answer && !currentWord.equals("") && !currentTranslate.equals("")) {
                return "Слово успешно добавлено";
            } else {
                return "Что-то пошло не так, слово не добавлено.";
            }
        } else {
            currentWord = "";
            currentTranslate = "";
            return "Хорошо, не будем добавлять.";
        }
    }

    public class CheckWordInBaseHold {
        private boolean exist;
        private String resultStr;

        public CheckWordInBaseHold(String resultStr, boolean exist) {
            this.resultStr = resultStr;
            this.exist = exist;
        }

        public String getResultStr() {
            return resultStr;
        }

        public boolean isExist() {
            return exist;
        }
    }

    public CheckWordInBaseHold checkWordInBase(String text) {
        ArrayList<String> answer = getRequest(checkWord(text));
        String mystr = "";
        if (answer.size() != 0) {
            for (String str : answer) {
                String arr[] = str.split(";");
                mystr = getTranslatedString(arr);
            }
            return new CheckWordInBaseHold(mystr, true);
        } else {
            String translate = "";
            try {
                translate = Translator.translate(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentWord = text;
            currentTranslate = translate;
            return new CheckWordInBaseHold("Cлова \"" + text + "\" с переводом \"" + translate + "\" нет в словаре! Добавить?", false);
        }
    }

    public String checkTranslateInBase(String text) {
        String mystr = "";
        ArrayList<String> answer = getRequest(checkTranslate(text));
        if (answer.size() != 0) {
            for (String str : answer) {
                String arr[] = str.split(";");
                mystr = mystr + text + " - " + arr[0] + System.lineSeparator();
            }
        }
        return mystr;
    }


    public void onUpdateReceived(Update update) {
        ArrayList<String> buttonList = new ArrayList<>();
        buttonList.add("/test");
        buttonList.add("/last");
        buttonList.add("/video");

        ArrayList<String> buttonListAdd = new ArrayList<>();
        buttonListAdd.add("/yes");
        buttonListAdd.add("/no");

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();
            if ("/test".equals(text)) {
                sendMsg(message, getResultStr(getRequest(getRandomWord()), false), false, buttonList);
            } else if ("/last".equals(text)) {
                sendMsg(message, getResultStr(getRequest(getLastWordWord()), false), false, buttonList);
            } else if ("/video".equals(text)) {
                sendMsg(message, getResultStr(getRequest(getRandomVideo()), true), false, buttonList);
            } else if ("/yes".equals(text)) {
                sendMsg(message, addNewWordResult(message.getFrom().getUserName(), true), false, buttonList);
            } else if ("/no".equals(text)) {
                sendMsg(message, addNewWordResult(message.getFrom().getUserName(), false), false, buttonList);
            } else {
                // если ввели слово (не команда), тогда проверим его наличие в бд и выведем соотв.результат
                if (getLang(text) == "en") {
                    sendMsg(message, checkWordInBase(text).getResultStr(), true, (checkWordInBase(text).isExist()) ? buttonList : buttonListAdd);
                }
                if (getLang(text) == "ru") {
                    sendMsg(message, checkTranslateInBase(text), true, buttonList);
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
        return properties.getProperty("telegramBotUsername");
    }

    public String getBotToken() {
        return properties.getProperty("telegramBotToken");
    }

    String getLang(String s) {
        char ch = s.trim().charAt(0);
        if ((ch >= 0x0041 && ch <= 0x005A) || (ch >= 0x0061 && ch <= 0x007A)) {
            return "en";
        }
        if ((ch >= 0x0410 && ch <= 0x044F) || ch == 0x0401 || ch == 0x0451) {
            return "ru";
        }
        throw new IllegalArgumentException("строка начинается с символа неизвестного языка");
    }

    public static ArrayList<String> getRequest(String sqlText) {
        ArrayList<String> result = new ArrayList<String>();
        Statement myStmt = null;
        try {
            if (!myCon.isValid(0)) {
                setConnection(properties.getProperty("jdbc.url"), properties.getProperty("jdbc.username"), properties.getProperty("jdbc.password"));
            }
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
        return "SELECT tb1.word, tb1.translate1, tb1.translate2, tb1.translate3, tb1.translate4, tb1.context from words AS tb1 " +
                "INNER JOIN ( SELECT DISTINCT word, translate1, translate2, translate3, translate4, context from words " +
                "where context LIKE '%http%'  order by RAND() LIMIT 1) AS tb2 " +
                "ON tb1.context = tb2.context";
    }

    public static String getRandomWord() {
        return "SELECT word, translate1, translate2, translate3, translate4, context from words order by RAND() LIMIT 30";
    }

    public static String getLastWordWord() {
        return "SELECT word, translate1, translate2, translate3, translate4, context from words ORDER BY `id_word` DESC LIMIT 30";
    }

    public static String checkWord(String text) {
        return "SELECT word, translate1, translate2, translate3, translate4, context from words where TRIM(word) = '" + text + "'";
    }

    public static String checkTranslate(String text) {
        return "SELECT word, translate1, translate2, translate3, translate4, context from words " +
                "where TRIM(translate1) = '" + text + "'" + " OR TRIM(translate2) = '" + text + "'" + " OR TRIM(translate3) = '" + text + "'" + " OR TRIM(translate4) = '" + text + "'";
    }

    public static String addNewWord(String text, String translate1, String context) {
        return "INSERT INTO `words` (`id_word`, `word`, `translate1`, `translate2`, `translate3`, `translate4`, `context`) " +
                "VALUES (NULL, '" + text + "', '" + translate1 + "', '', '', '', '" + context + "')";
    }

}

