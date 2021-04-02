package com.you.company;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The class to parse the html to a queue.
 */
public class HtmlParser {
    Logger logger = LoggerFactory.getLogger(getClass());
    private Queue<String> resultQueue = new LinkedList<>();
    private StringBuilder element = null;
    private StringBuilder content = null;
    private String html;

    /**
     * Constuctor
     * @param html The html to parse
     */
    public HtmlParser(String html) {
        this.html = html;
    }

    /**
     * parse the html
     * @return
     */
    public Queue<String> parse() {
        logger.debug("parse#start");
        for (int pos = 0; pos < html.length(); pos++) {
            char c = html.charAt(pos);

            if (c == '<') {
                htmlElementStart(c);
            } else if (c == '>') {
                htmlElementEnd(c);
            } else {
                if (element != null) {
                    element.append(c);
                }
                if (content != null) {
                    content.append(c);
                }
            }
        }
        debugOutput();

        return resultQueue;
    }


    /**
     * The handling of when c is <
     * @param c the current char
     * @return true - continue next loop, false - execute rest codes.
     */
    private void htmlElementStart(char c) {
        logger.debug("htmlElementStart#start");
        if (content != null) {
            resultQueue.add(content.toString());
            logger.debug("content:" + content);
        }

        content = null;
        element = new StringBuilder();
        element.append(c);
    }

    /**
     * The handling of when c is >
     * @param c the current char
     * @return true - continue next loop, false - execute rest codes.
     */
    private void htmlElementEnd(char c) {
        logger.debug("htmlElementEnd#start");
        element.append(c);
        resultQueue.add(element.toString());
        logger.debug("element:" + element);
        element = null;
        content = new StringBuilder();
    }

    /**
     * Debug log
     */
    private void debugOutput() {
        for (String item: resultQueue) {
            logger.debug("queue item:" + item);
        }
    }
}
