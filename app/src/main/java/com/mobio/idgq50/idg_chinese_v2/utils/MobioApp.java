package com.mobio.idgq50.idg_chinese_v2.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.maruboshi.driverguide.epub.XMLParser;
import com.maruboshi.driverguide.model.EpubInfo;
import com.mobio.idgq50.idg_chinese_v2.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MobioApp {

    public static MobioApp instance;
    private Configuration conf;
        public String Vuforia_key = "AZeJ28P/////AAAAAcTlmrGvj0ngjCcwQjIavJcqQvIaBitAo5eBTkG2cG4PyF+t82bIUefZWp/9apWQuelfAA08QuTLCad92KWOLHBY+d1EzDYRAVelFk0LDUkFeW7TrxowR1fPpEu1Axlxm9NA6j9YMSqfxxH/378ei+xhkeUqLJeSTThdQVIivZ2WjlE6hIhhwZS0nqh/0mHH92gbfzMcgdrLQ1uM5u0jIRn9yjbVw4/CzCtQzjYpRyqWRz+vhrGMFL9xKBqyTwRy2PKrR2/6T6xYER4ZyCddIGJD5zgrLxPkPp3dbFGslnmTU3ZZYijn2kMIROhcIZYmx8F59odWmfNh4lRcV91pK554tGieRVsWzFqB/APdo3Av"; // free
//    private String Vuforia_key = "ATZPXEj/////AAAAAK8J10rSI0dLr0sGOY7rQdUf20QxKpYGh6/JG14CkV1pjGLTF81OoLa/hx0u5+ZWxIO2Iufir6M2cEX2zz7GY3UGxAsJ6mmtoricZVrENjMjGhzYpIE6Rbuk62hcaRcrdy9dFM1CITHzbDPEE7cEcY99HHSSLHzsfLR9Zlo/WNzc5aTcOoWl+hewMmLfyZdwK/KN36FlvLy0j4hzM68fitZSgXvo+Ed1gCDDpSTT+yfF4r8h11emgrs68kLzUl4WWH/xwZbupVQj4cyGag2sC3vu2b2qSuyABHXfTViQjsIqUL8r3I7wF6Xkio8XN+a6b1Y3IArX4FJ9XlB3VR6QxCyFjUYbh96CSBEDA3Q8Q5wd"; // Paid key for NDG

    public static MobioApp getInstance() {
        if (instance == null) {
            instance = new MobioApp();
        }
        return instance;
    }

    public void setCarImage(int position, ImageView imageView) {
        switch (position) {
            case 1:
                //imageView.setBackgroundResource(R.drawable.q30_me);
                break;
            case 2:
                //imageView.setBackgroundResource(R.drawable.car_default);
                break;
            case 3:
            case 4:
            default:
                break;
        }
    }

    public String getCarPath(int carType) {
        String carPath = "";
        switch (carType) {
            case 1:
                carPath = Values.PATH + Values.qx30_folder;
                break;
            case 2:
                carPath = Values.PATH + Values.q50_folder;
                break;
            case 100:
                carPath = Values.PATH;
            default:
                break;
        }

        return carPath;
    }

    public String getModelPath(int carType) {
        String carPath = "";
        switch (carType) {
            case 2:
                carPath = Values.MODELPATH + Values.q50_folder;
                break;
            default:
                break;
        }

        return carPath;
    }

    public String getePubFolderPath(int carType) {
        String path = "";
        switch (carType) {
            case 1:
                path = Values.qx30_folder;
                break;

            case 2:
                path = Values.q50_folder;
                break;

            default:
                break;
        }

        return path;
    }

    public boolean isEpubExists(String ePubPath, String lang) {
        if (new File(ePubPath + "/" + Values.homepage + lang + Values.epub).exists()
                && new File(ePubPath + "/" + Values.button + lang + Values.epub).exists()
                && new File(ePubPath + "/" + Values.combimeter + lang + Values.epub).exists()
            /*&& new File(ePubPath + "/" + Values.info + lang + Values.epub).exists()*/) {
            return true;
        } else {
            return false;
        }
    }

    public String getCarName(int car) {

        String cn = "";
        if (car == 1) {
            cn = "QX30";
        } else if (car == 2) {
            cn = "Q50";
        } else {
            cn = "";
        }
        return cn;
    }

    public String getLanguageName(String language_name) {
        String cn = "";
        if (language_name.equalsIgnoreCase("en")) {
            cn = "English";
        } else if (language_name.equalsIgnoreCase("ar")) {
            cn = "Arab";
        } else {
            cn = "English";
        }

        return cn;
    }

    public Configuration changeLocalLanguage(Activity activity, String lang) {

        conf = activity.getResources().getConfiguration();

        if (!lang.equals("")) {
            if (lang.contentEquals("en")) {
                conf.locale = new Locale("en");
            } else {
                conf.locale = new Locale("en");
            }
        }
        return conf;
    }

    public boolean createPath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public void setLocale(Context context, String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    public ArrayList<EpubInfo> parseePub(String dest) {

        Logger.error("destination", "___________" + dest);

        ArrayList list = new ArrayList();
        XMLParser parser = new XMLParser();
        String xml = this.getFileContent(dest + Values.TOC_DIRECTORY);
        Document doc = parser.getDomElement(xml);
        NodeList nl = doc.getElementsByTagName("navPoint");

        for (int i = 0; i < nl.getLength(); ++i) {
            EpubInfo info = new EpubInfo();
            Element element = (Element) nl.item(i);
            info.setIndex(i);
            info.setHtmlLink(parser.getAttributeValue(element, "content", "src"));
//            info.setTitle(parser.getValue(element, "text"));
//            info.setSearchTag(parser.getValue(element, "search"));

            info.setTitle(parser.getValue(element, "text"));
            info.setSearchTag(parser.getValue(element, "search"));

            list.add(info);
        }

        return list;
    }

    private String getFileContent(String targetFilePath) {
        FileInputStream fileInputStream = null;
        File file = new File(targetFilePath);

        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException var8) {
            var8.printStackTrace();
        }

        StringBuilder sb = null;

        try {
            for (; fileInputStream.available() > 0; sb.append((char) fileInputStream.read())) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
            }
        } catch (IOException var9) {
            var9.printStackTrace();
        }

        String fileContent = "";
        if (sb != null) {
            fileContent = sb.toString();
        }

        try {
            fileInputStream.close();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return fileContent;
    }

}
