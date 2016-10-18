package org.devnexus.dn_tv_generator;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.apache.commons.io.IOUtils;

public class Main {

    private static final String ROBOTO = "/RobotoCondensed-Regular.ttf";
    private static final String DEVNEXUS_LOGO = "/ct.png";
    private static final int PADDING_TOP = 27;
    private static final int PADDING_LEFT = 48;
    private static final Color BACKGROUND_COLOR = Color.BLACK;
    private static final String THEME_YELLOW = "#f0ba33";
    private static final String THEME_GRAY = "#403c49";

    private static final String SCHEDULE = "/tvScheduleData.json";
    private static final ExecutorService es = Executors.newFixedThreadPool(1);

    private static final List<Callable<Object>> todo = new ArrayList<>(5);

    private static final BufferedImage logo;

    static {
        try {
            logo = ImageIO.read(Main.class.getResourceAsStream(DEVNEXUS_LOGO));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String args[]) throws Exception {

        HashMultimap<String, JsonObject> roomSessions = HashMultimap.<String, JsonObject>create(30, 30);
        Map<String, Integer> roomColors = new HashMap<String, Integer>(30);

        String scheduleJSON = IOUtils.toString(Main.class.getResourceAsStream(SCHEDULE));
        JsonElement scheduleJson = new JsonParser().parse(scheduleJSON);

        JsonArray items = scheduleJson.getAsJsonObject().get("scheduleItems").getAsJsonArray();
        JsonObject item;
        for (int i = 0; i < items.size(); i++) {
            item = items.get(i).getAsJsonObject();
            JsonObject room = item.get("room").getAsJsonObject();
            String roomName = room.get("name").getAsString();
            Integer roomColor = Integer.parseInt(room.get("color").getAsString().replace("#", ""), 16);
            roomSessions.put(roomName, item);
            roomColors.put(roomName, roomColor);

        }

        Calendar cal = Calendar.getInstance();
        cal.set(2016, Calendar.OCTOBER, 20, 1, 0, 0); //Year, month, day of month, hours, minutes and seconds
        generateOneDay(roomSessions, roomColors, cal.getTime(),  "Thursday");
        cal.set(2016, Calendar.OCTOBER, 21, 1, 0, 0); //Year, month, day of month, hours, minutes and seconds
        generateOneDay(roomSessions, roomColors, cal.getTime(),  "Friday");
        cal.set(2016, Calendar.OCTOBER, 22, 1, 0, 0); //Year, month, day of month, hours, minutes and seconds
        generateOneDay(roomSessions, roomColors, cal.getTime(),  "Saturday");

    }

    private static void generateOneDay(HashMultimap<String, JsonObject> roomSessions, Map<String, Integer> roomColors, Date specificDayDate, String dateName) throws IOException, FontFormatException {
        Font fontHuge = Font.createFont(Font.TRUETYPE_FONT, Main.class.getResourceAsStream("/Roboto-Bold.ttf"));
        fontHuge = fontHuge.deriveFont(96F);

        Font font = Font.createFont(Font.TRUETYPE_FONT, Main.class.getResourceAsStream(ROBOTO));
        font = font.deriveFont(48F);

        Font fontSmall = Font.createFont(Font.TRUETYPE_FONT, Main.class.getResourceAsStream(ROBOTO));
        fontSmall = fontSmall.deriveFont(36F);

        Font fontTiny = fontSmall.deriveFont(24F);

        for (String roomName : roomSessions.keySet()) {

            BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
            Graphics imageGfx = image.getGraphics();

            fillBackGround(imageGfx);
            drawLogo(imageGfx);

            int roomColor = roomColors.get(roomName);
            ArrayList<JsonObject> sessions = new ArrayList<JsonObject>(roomSessions.get(roomName));
//            if (sessions.size() < 2) {
//                continue;
//            }
//
//            if (sessions.get(1).get("presentation") == null || sessions.get(1).get("presentation").isJsonNull() || sessions.get(1).get("presentation").getAsJsonObject().get("track") == null) {
//                continue;
//            }

            String trackName = sessions.get(0).get("presentation").getAsJsonObject().get("track").getAsJsonObject().get("name").getAsString();

            ((Graphics2D) imageGfx).setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            imageGfx.setColor(Color.decode(roomColor + ""));

            imageGfx.setFont(fontHuge);
            imageGfx.drawString(trackName, 1920 - 80 - (int) imageGfx.getFontMetrics().getStringBounds(trackName, imageGfx).getWidth(), PADDING_TOP + 80);

            Collections.sort(sessions, (JsonObject object, JsonObject another)
                    -> (int) (object.get("fromTime").getAsLong() - another.get("fromTime").getAsLong()));

            int width = 450;
            int height = 400;
            int offset_x = 20;
            int offset_y = 180;

            for (JsonObject session : sessions) {
//                if (session.get("presentation") == null) {
//                    continue;
//                }
                DateFormat format = new SimpleDateFormat("E h:mm a");
                BufferedImage tile = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                long thisSessionDateMillis = session.get("fromTime").getAsLong();
                Date thisSessionDate = new Date(thisSessionDateMillis);
                Date almostTomorrow = new Date(specificDayDate.getTime() +(1000*60*60*22));

                // TEST IF IT IS ON THIS DATE
                if ( thisSessionDate.before(almostTomorrow) && thisSessionDate.after(specificDayDate))  {

                    String date = format.format(new Date(session.get("fromTime").getAsLong() ));
                    Graphics2D tileGraphics = (Graphics2D) tile.getGraphics();
                    tileGraphics.setRenderingHint(
                            RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

//                tileGraphics.setColor(Color.decode(roomColor + ""));
                    tileGraphics.setColor(Color.decode(THEME_GRAY + ""));

                    tileGraphics.fill3DRect(0, 0, width, height, true);

                    tileGraphics.setColor(Color.decode(THEME_YELLOW + ""));
                    tileGraphics.setFont(font);
                    tileGraphics.drawString(date, 20, 50);

                    tileGraphics.setFont(fontSmall);
                    String sessionTitle = "";
                    if (session.get("presentation").isJsonNull()) {
                        sessionTitle = session.get("title").getAsString();
                    } else {
                        sessionTitle = session.get("presentation").getAsJsonObject().get("title").getAsString();
                    }
                    List<String> textList = StringUtils.wrap(sessionTitle, tileGraphics.getFontMetrics(), width - 80);
                    if (textList.size() > 3) {
                        textList = textList.subList(0, 3);
                        String lastLine = textList.get(2);
                        lastLine = lastLine.subSequence(0, lastLine.length() - 3).toString() + "...";
                        textList.remove(2);
                        textList.add(lastLine);
                    }

                    tileGraphics.setFont(fontSmall);
                    for (int i = 0; i < textList.size(); i++) {
                        tileGraphics.drawString(textList.get(i), 20, 110 + 40 * i);
                    }

                    tileGraphics.setFont(fontTiny);
                    int startSpeakers = 170 + (textList.size() - 1) * 30;

                    JsonArray speakersArray;
                    if (session.get("presentation").isJsonNull()) {
                        speakersArray = new JsonArray();
                    } else {
                        speakersArray = session.get("presentation").getAsJsonObject().get("speakers").getAsJsonArray();
                    }

                    List<String> speakerNames = new ArrayList<>();
                    for (int i = 0; i < speakersArray.size(); i++) {
                        JsonObject speaker = speakersArray.get(i).getAsJsonObject();
                        speakerNames.add(speaker.get("firstName").getAsString() + " " + speaker.get("lastName").getAsString());
                    }

                    tileGraphics.setFont(fontTiny);

                    for (int i = 0; i < speakerNames.size(); i++) {
                        tileGraphics.drawString(speakerNames.get(i), 20, startSpeakers + i * 30);
                    }

                    imageGfx.drawImage(tile, offset_x, offset_y, null);
                    offset_x += width + 20;
                    if (offset_x > 1800) {
                        offset_x = 20;
                        offset_y = offset_y + height + PADDING_TOP;
                    }
                    write(dateName, roomName, image);
                } else {

                }

            }



        }
    }
    private static Color asColor(int color) {
        return new Color(color, true);
    }

    private static void drawLogo(Graphics imageGfx) throws IOException {

        imageGfx.setColor(asColor(Color.TRANSLUCENT));
        imageGfx.drawImage(logo, PADDING_LEFT, PADDING_TOP + 30, null);

    }

    private static void fillBackGround(Graphics imageGfx) {
        imageGfx.setColor(BACKGROUND_COLOR);
        imageGfx.fillRect(0, 0, 1920, 1080);
    }

    public static boolean test(JsonObject session, Date date) {
        return session.get("fromTime").getAsLong() < date.getTime();
    }

    private static void write(String dateName, String roomName, RenderedImage image) {
        String roomNameEscaped = roomName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(new File("/Users/prpatel/Pictures/DevNexus/" + dateName + "_" + roomNameEscaped  + ".jpg"))) {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
            ImageWriter writer;
            writer = iter.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(0.99f);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), iwp);
            writer.dispose();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

}
