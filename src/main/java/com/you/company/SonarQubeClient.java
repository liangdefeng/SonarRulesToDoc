package com.you.company;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.rules.SearchRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A class responsible for calling SonarQube API and convert the json result ot Java object.
 */
public class SonarQubeClient {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The SonarQube web site url.
     */
    private String url = null;

    /**
     * The token to invoke SonarQube API.
     */
    private String token = null;

    /**
     * The language to search.
     */
    private String language = null;

    /**
     * The page size
     */
    private String pageSize = "500";

    /**
     * The type of the rules.
     */
    private String type = "VULNERABILITY";

    /**
     * Constructor
     */
    public SonarQubeClient(String url, String token, String language) {
        this.url = url;
        this.token = token;
        this.language = language;
    }

    /**
     * Search SonarQube and get the rules.
     * @return SearchResponse
     */
    public Rules.SearchResponse search() {
        logger.debug("search#start");
        if (language == null) {
            logger.error("language is null.");
            return null;
        }

        WsClient wsClient = WsClientFactories.getDefault()
                .newClient(HttpConnector.newBuilder()
                        .url(url)
                        .token(token)
                        .build());

        SearchRequest request = new SearchRequest();

        // set languages
        List<String> languages = new ArrayList<>();
        languages.add(language);
        request.setLanguages(languages);

        // set types
        List<String> types = new ArrayList<>();
        types.add(type);
        request.setTypes(types);
        // set Ps
        request.setPs(pageSize);

        Rules.SearchResponse searchResponse = wsClient.rules().search(request);
        logger.info(searchResponse.getRulesList().size() + " rules");
        return searchResponse;
    }
}
