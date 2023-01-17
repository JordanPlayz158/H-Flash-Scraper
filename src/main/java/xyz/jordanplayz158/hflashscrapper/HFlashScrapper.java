package xyz.jordanplayz158.hflashscrapper;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class HFlashScrapper {
    private static final String DOMAIN = "https://h-flash.com";
    private static int lastPage = 2;

    private static final String SCRIPT_VARIABLE = "downpath";
    private static final File LOG_FILE = new File("log.log");

    public static void main(String[] args) throws IOException {
        PrintWriter logWriter = new PrintWriter(LOG_FILE);

        for (int i = 1; i < lastPage; i++) {
            int finalI = i;
            getPageUrls(i).forEach(url -> {
                try {
                    Document gamePage = Jsoup.connect(DOMAIN + url).get();

                    AtomicReference<String> variablesScriptAtomic = new AtomicReference<>(null);

                    gamePage.head().getElementsByTag("script").forEach(script -> {
                        String scriptData = script.data();

                        if(scriptData.contains(SCRIPT_VARIABLE)) {
                            variablesScriptAtomic.set(scriptData);
                        }
                    });

                    String variablesScript = variablesScriptAtomic.get();

                    if(variablesScript == null) {
                        return;
                    }

                    String downPath = getJavaScriptVariable(variablesScript, SCRIPT_VARIABLE);

                    // Yo what's up PokemonHacker

                    ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(DOMAIN + downPath).openStream());

                    String downName = StringEscapeUtils.unescapeJava(getJavaScriptVariable(variablesScript, "downname"));

                    File swfSaveDirectory = new File("/mnt/HDD/H-Flash_Games");

                    File swfFile = new File(swfSaveDirectory, downName);

                    if(!swfFile.exists()) {
                        FileOutputStream fileOutputStream = new FileOutputStream(swfFile);
                        FileChannel fileChannel = fileOutputStream.getChannel();

                        fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                        logWriter.printf("[Page %d of %d] Game '%s' was successfully downloaded!%n", finalI, lastPage, downName);
                    } else {
                        logWriter.printf("[Page %d of %d] Game '%s' was skipped due to file already existing!%n", finalI, lastPage, downName);
                    }

                    logWriter.flush();
                } catch (IOException e) {
                    logWriter.println();
                    logWriter.printf("Game from URL '%s' failed to be retrieved!%n", url);
                    e.printStackTrace(logWriter);
                    logWriter.println();

                    logWriter.flush();
                }
            });
        }

        logWriter.close();
    }

    private static List<String> getPageUrls(int pageNumber) throws IOException {
        Document document = Jsoup.connect(DOMAIN + "/all/" + pageNumber).get();

        lastPage = getLastPage(document);

        return document.getElementsByClass("gamebox").eachAttr("href");
    }

    private static int getLastPage(Document document) {
        Element lastPageHrefElement = document.getElementById("pagelink_last");

        if(lastPageHrefElement == null || !lastPageHrefElement.hasAttr("href")) {
            return lastPage;
        }

        String lastPageHref = lastPageHrefElement.attr("href");

        return Integer.parseInt(lastPageHref.substring(lastPageHref.lastIndexOf("/") + 1));
    }

    private static String getJavaScriptVariable(String javaScriptText, String variableName) {

        // I replaced all strings to empty air so I didn't need to rely on consistent formatting
        // But then some games, actually have proper spaces in their download paths
        // Which caused some of them to fail
        String swfPathLeft = javaScriptText.substring(javaScriptText.indexOf(variableName) + variableName.length());

        // Remove closing " as variable is string
        String swfPathRight = swfPathLeft.substring(0, swfPathLeft.indexOf(";") - 1);
        // Remove starting "
        return StringEscapeUtils.unescapeJava(swfPathRight.substring(swfPathRight.indexOf("=") + 3));
    }
}