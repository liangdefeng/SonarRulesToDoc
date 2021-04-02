package com.you.company;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.Rules;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * The class is responsible to generate documents according to a Java object created by SonarQube API reader.
 */
public class DocGenerator {

    Logger logger = LoggerFactory.getLogger(getClass());

    private static final String BLOCKQUOTE = "blockquote";
    private static final String ARIAL = "Arial";
    private static final String CALIBRI = "Calibri";

    private StringBuilder separateLineBuff = null;
    private Set<String> ignoreElementSet = new HashSet<>();
    private Deque<String> elementStack = new LinkedList<>();
    private String currentItem;
    private XWPFDocument document;
    private XWPFParagraph paragraph = null;
    private String linkUrl = null;
    private int cursorIndex = 0;

    /**
     * The main method
     * @param args
     */
    public static void main(String[] args) {
        List<String> languages = new ArrayList<>();
        languages.add("tsql");
        languages.add("php");
        languages.add("objc");
        languages.add("js");
        languages.add("ts");
        languages.add("java");
        languages.add("web");
        languages.add("cs");
        languages.add("cpp");
        languages.add("c");

        for (String language: languages) {
            new DocGenerator().generateWordFile(language);
        }
    }

    /**
     * Generate the word file according to languageindexOf.
     * @param language
     * @return
     */
    public boolean generateWordFile(String language) {
        logger.info("generateWordFile#start");
        try {
            document = new XWPFDocument(new FileInputStream("templates/template.docx"));
            setCursorIndex();

            ignoreElementSet.add("br");
            ignoreElementSet.add("ul");

            // Get the result rules and sort
            Rules.SearchResponse response = getClient(language).search();
            List<Rules.Rule> ruleLists = new ArrayList<>();
            ruleLists.addAll(response.getRulesList());
            ruleLists.sort(Comparator.comparing(Rules.Rule::getName));

            // loop the rules in the sorted list.
            for(Rules.Rule rule: ruleLists) {

                // add title
                addItemTitle(rule);

                // purse the html
                Queue<String> queue = new HtmlParser(rule.getHtmlDesc()).parse();
                currentItem = queue.poll();
                while (currentItem != null) {
                    logger.debug("currentItem:" + currentItem);
                    if (!currentItem.trim().isEmpty()) {
                        processItem();
                    }
                    currentItem = queue.poll();
                }
            }

            // update the TOC
            document.enforceUpdateFields();

            // The stack size should be zero.
            logger.info("stack size:" + elementStack.size());

            // output file.
            String filePath = "output/Security_Programming_Guideline_for_" + language.toUpperCase() +".docx";
            OutputStream fileOut = new FileOutputStream(filePath);
            document.write(fileOut);

            logger.info("File created:" + filePath);
            logger.info("Finished");
            return true;
        } catch (FileNotFoundException e) {
            logger.error(e.toString());
        } catch (IOException e) {
            logger.error(e.toString());
            logger.error(e.getMessage());
        }
        return false;
    }

    /**
     * Get SonarQube client.
     * @param language the language
     * @return Get SonarQube client.
     */
    private SonarQubeClient getClient(String language) {
        logger.debug("getClient#start");
        String url = Util.getInstance().get("sonar1.url");
        String token = Util.getInstance().get("sonar1.token");
        return new SonarQubeClient(url, token, language);
    }

    /**
     * Find the ${rules} in the document and set the cursorIndex to it.
     */
    private void setCursorIndex() {
        logger.debug("setCursorIndex#start");
        int i = 0;
        for (XWPFParagraph para: document.getParagraphs()) {
            if (para.getText().trim().equals("${rules}")) {
                cursorIndex = i;
                para.removeRun(0);
            }
            i++;
        }
    }

    /**
     * Create new paragraph
     * @return
     */
    private XWPFParagraph generateNewParagraph() {
        logger.debug("generateNewParagraph#start");
        cursorIndex += 1;
        XWPFParagraph xwpfParagraph = document.getParagraphArray(cursorIndex);
        XmlCursor xmlCursor = xwpfParagraph.getCTP().newCursor();
        return document.insertNewParagraph(xmlCursor);
    }

    /**
     * Process element one by one.
     */
    private void processItem() {
        logger.debug("processItem#start");

        if (currentItem.trim().equals("</ul>")) {
            paragraph = generateNewParagraph();
            paragraph.setAlignment(ParagraphAlignment.LEFT);
        }

        for (String element : ignoreElementSet) {
            if (currentItem.trim().equals("<" + element + ">")
                    || currentItem.trim().equals("</" + element + ">")) {
                logger.debug("ignore element found. - " + currentItem);
                return;
            }
        }

        if (currentItem.trim().startsWith("<")) {
            logger.debug("currentItem:" + currentItem);
            processHtmlElement();

        } else {
            if (!currentItem.trim().isEmpty()) {
                processParagraph();
            }
        }

    }

    /**
     * Process html element.
     */
    private void processHtmlElement() {
        logger.debug("processHtmlElement#start");
        if (currentItem.trim().startsWith("</")) {
            processEndElement();
        } else {
            processOpenElement();
        }
    }

    /**
     * Process end html element.
     */
    private void processEndElement() {
        logger.debug("processEndElement#start");
        String htmlElement = currentItem.trim().substring(2, currentItem.trim().length() - 1);
        if (elementStack.peek().equals(htmlElement)) {

            addCarriageReturn();

            elementStack.pop();
            logger.debug("pop htmlElement:" + htmlElement);

            if (elementStack.isEmpty()) {
                paragraph = null;
            }
        } else {
            logger.debug("htmlElement not found:" + htmlElement);
        }
    }

    /**
     * Process open html element.
     */
    private void processOpenElement() {
        logger.debug("processOpenElement#start");
        String currentText = currentItem.trim().replaceAll("\n", " ");
        int index = currentText.indexOf(' ');
        if (index < 0) {
            index = currentText.indexOf('>');
        }

        String htmlElement = currentText.substring(1, index);

        if (htmlElement.equals("a")) {
            int start = currentText.trim().indexOf("href=\"");
            linkUrl = currentText.trim().substring(start + 6, currentItem.trim().length() - 2);
        }

        if (elementStack.isEmpty()) {
            generateNewParagraph(htmlElement);
        }

        elementStack.push(htmlElement);
        logger.debug("push htmlElement:" + htmlElement);
    }

    /**
     * Create Paragraph according to html element according to html element.
     * @param htmlElement html element.
     */
    private void generateNewParagraph(String htmlElement) {
        logger.debug("createParagraph#start");
        switch (htmlElement) {
            case "p":
            case "pre":
                paragraph = generateNewParagraph();
                paragraph.setAlignment(ParagraphAlignment.LEFT);
                break;
            case "li":
                paragraph = generateNewParagraph();
                paragraph.setAlignment(ParagraphAlignment.LEFT);
                XWPFAbstractNum xwpfAbstractNum = document.createNumbering().getAbstractNum(BigInteger.valueOf(1));
                XWPFNumbering numbering = document.createNumbering();
                BigInteger abstractNumID = numbering.addAbstractNum(xwpfAbstractNum);
                BigInteger numID = numbering.addNum(abstractNumID);
                paragraph.setNumID(numID);
                break;
            case BLOCKQUOTE:
                paragraph = generateNewParagraph();
                paragraph.setAlignment(ParagraphAlignment.LEFT);
                paragraph.setStyle("Quote");
                break;
            case "h2":
                paragraph = generateNewParagraph();
                paragraph.setAlignment(ParagraphAlignment.LEFT);
                paragraph.setStyle("Heading3");
                break;
            default:
                // do nothing.
        }
    }

    /**
     * Process the paragraph according to the html element on the top of the queue.
     */
    private void processParagraph() {
        logger.debug("processParagraph#start");
        if (!elementStack.isEmpty() && paragraph != null) {

            String element = elementStack.peek();
            if (element != null) {
                switch (element) {
                    case "a":
                        addHyperlink();
                        break;
                    case BLOCKQUOTE:
                    case "p":
                        processP();
                        break;
                    case "li":
                        processLi();
                        break;
                    case "strong":
                    case "code":
                        processCode();
                        break;
                    case "pre":
                        processPre();
                        break;
                    case "h2":
                        processH2();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Add a new line to current paragraph.
     */
    private void addCarriageReturn() {
        logger.debug("addCarriageReturn#start");
        if (!elementStack.isEmpty() && paragraph != null) {

            String element = elementStack.peek();
            if (element != null) {
                switch (element) {
                    case BLOCKQUOTE:
                    case "pre":
                        paragraph.createRun().addCarriageReturn();
                        break;
                    case "p":
                        if (!elementStack.getFirst().equals(BLOCKQUOTE)) {
                            paragraph.createRun().addCarriageReturn();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * The logic to translate the content in html code element in to word.
     */
    private void processCode() {
        logger.debug("processCode#start");
        XWPFRun textRun = paragraph.createRun();
        textRun.setText(StringEscapeUtils.unescapeHtml4(currentItem.replace("\n", "")));
        textRun.setFontFamily(ARIAL);
        textRun.setBold(true);
        textRun.setFontSize(11);
    }

    /**
     * The logic to translate the content in html li element in to word.
     */
    private void processLi() {
        logger.debug("processLi#start");
        XWPFRun textRun = paragraph.createRun();
        textRun.setText(StringEscapeUtils.unescapeHtml4(currentItem.replace("\n", "")));
        textRun.setFontFamily(CALIBRI);
        textRun.setFontSize(11);
    }

    /**
     * The logic to translate the content in html p element in to word.
     */
    private void processP() {
        logger.debug("processP#start");
        XWPFRun textRun = paragraph.createRun();
        textRun.setText(StringEscapeUtils.unescapeHtml4(currentItem.replace("\n", " ")));
        textRun.setFontFamily(CALIBRI);
        textRun.setFontSize(11);
    }

    /**
     * The logic to translate the content in html pre element in to word.
     */
    private void processPre() {
        logger.debug("processPre#start");

        XWPFRun textRun = paragraph.createRun();
        textRun.setText(separateLine());
        textRun.setFontFamily(ARIAL);
        textRun.setFontSize(11);

        String[] items = currentItem.split("\n");
        for (String lineString: items) {

            textRun = paragraph.createRun();
            textRun.setText(StringEscapeUtils.unescapeHtml4(lineString));
            textRun.setFontFamily(ARIAL);
            textRun.setFontSize(9);
            textRun.addCarriageReturn();
        }

        textRun = paragraph.createRun();
        textRun.setText(separateLine());
        textRun.setFontFamily(ARIAL);
        textRun.setFontSize(11);
    }

    /**
     * Construct a separate line.
     */
    private String separateLine() {
        logger.debug("separateLine#start");
        if (separateLineBuff == null) {
            separateLineBuff = new StringBuilder();
            for (int i=0; i < 120;i++ ) {
                separateLineBuff.append("-");
            }
        }
        return separateLineBuff.toString();
    }

    /**
     * Appends an external hyperlink to the paragraph.
     */
    public void addHyperlink() {
        logger.debug("addHyperlink#start");

        //Add the link as External relationship
        String id = document.getPackagePart()
                .addExternalRelationship(linkUrl, XWPFRelation.HYPERLINK.getRelation())
                .getId();

        //Append the link and bind it to the relationship
        CTHyperlink cLink=paragraph.getCTP().addNewHyperlink();
        cLink.setId(id);

        //Create the linked text
        CTText ctText=CTText.Factory.newInstance();
        ctText.setStringValue(currentItem.replaceAll("\n", ""));

        CTR ctr=CTR.Factory.newInstance();
        ctr.setTArray(new CTText[]{ctText});

        //Insert the linked text into the link
        cLink.setRArray(new CTR[]{ctr});

        XWPFHyperlinkRun hyperlinkrun = new XWPFHyperlinkRun(
                cLink,
                cLink.getRArray(0),
                paragraph
        );

        hyperlinkrun.setColor("0000FF");
        hyperlinkrun.setUnderline(UnderlinePatterns.SINGLE);

        XWPFRun textRun = paragraph.createRun();
        textRun.setText(" ");
    }

    /**
     * Add title
     * @param rule rule
     */
    private void addItemTitle(Rules.Rule rule) {
        logger.debug("addItemTitle#start");
        XWPFParagraph titleParagraph = generateNewParagraph();
        titleParagraph.setStyle("Heading2");
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(rule.getName());
        titleRun.setFontFamily("Calibri Light");
        titleRun.setBold(true);
        titleRun.setFontSize(14);
        titleRun.setUnderline(UnderlinePatterns.SINGLE);
        titleRun.addCarriageReturn();
    }

    /**
     * The logic to translate the content in html h2 element in to word.
     */
    private void processH2() {
        logger.debug("processH2#start");
        XWPFRun textRun = paragraph.createRun();
        textRun.setBold(true);
        textRun.setItalic(true);
        textRun.setFontFamily(CALIBRI);
        textRun.setFontSize(11);
        textRun.setText(currentItem.replaceAll("\n", ""));
    }
}

