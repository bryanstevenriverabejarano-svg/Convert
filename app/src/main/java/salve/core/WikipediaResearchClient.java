package salve.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cliente de investigacion dirigida sobre la API publica de Wikipedia. */
public final class WikipediaResearchClient {
    private static final String API = "https://es.wikipedia.org/w/api.php";
    private static final int MAX_RESULTS = 3;

    public ResearchResult research(String question) throws Exception {
        String query = sanitizeQuery(question);
        if (query.isEmpty()) {
            return new ResearchResult(question, Collections.emptyList());
        }

        String searchUrl = API + "?action=query&list=search&format=json&utf8=1&srlimit="
                + MAX_RESULTS + "&srsearch=" + encode(query);
        JSONObject searchRoot = new JSONObject(getJson(searchUrl));
        JSONArray matches = searchRoot.getJSONObject("query").getJSONArray("search");
        if (matches.length() == 0) {
            return new ResearchResult(question, Collections.emptyList());
        }

        List<Integer> pageIds = new ArrayList<>();
        for (int i = 0; i < matches.length() && i < MAX_RESULTS; i++) {
            pageIds.add(matches.getJSONObject(i).getInt("pageid"));
        }
        StringBuilder ids = new StringBuilder();
        for (Integer id : pageIds) {
            if (ids.length() > 0) ids.append('|');
            ids.append(id);
        }

        String extractUrl = API + "?action=query&prop=extracts&explaintext=1&exintro=1"
                + "&format=json&utf8=1&pageids=" + encode(ids.toString());
        JSONObject pages = new JSONObject(getJson(extractUrl))
                .getJSONObject("query").getJSONObject("pages");
        List<Source> sources = new ArrayList<>();
        for (Integer id : pageIds) {
            JSONObject page = pages.optJSONObject(String.valueOf(id));
            if (page == null) continue;
            String title = page.optString("title", "").trim();
            String extract = page.optString("extract", "").trim();
            if (title.isEmpty() || extract.isEmpty()) continue;
            if (extract.length() > 2_500) extract = extract.substring(0, 2_500);
            String sourceUrl = "https://es.wikipedia.org/?curid=" + id;
            sources.add(new Source(title, sourceUrl, extract));
        }
        return new ResearchResult(question, sources);
    }

    private String getJson(String rawUrl) throws Exception {
        NetworkResourcePolicy.Validation validation =
                NetworkResourcePolicy.validateKnowledgeUrl(rawUrl);
        if (!validation.allowed) throw new IllegalArgumentException(validation.reason);

        HttpURLConnection connection = (HttpURLConnection) new URL(validation.normalizedUrl)
                .openConnection();
        connection.setConnectTimeout(7_000);
        connection.setReadTimeout(7_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "SalveResearch/1.0");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Wikipedia respondio HTTP " + status);
            }
            if (connection.getContentLengthLong() > NetworkResourcePolicy.MAX_KNOWLEDGE_BYTES) {
                throw new IllegalStateException("Respuesta de investigacion demasiado grande");
            }
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8_192];
                int total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > NetworkResourcePolicy.MAX_KNOWLEDGE_BYTES) {
                        throw new IllegalStateException("Respuesta de investigacion demasiado grande");
                    }
                    output.write(buffer, 0, read);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            connection.disconnect();
        }
    }

    private String sanitizeQuery(String question) {
        if (question == null) return "";
        String value = question.replaceAll("[\\p{Cntrl}]", " ").trim();
        return value.length() > 240 ? value.substring(0, 240) : value;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException impossible) {
            throw new IllegalStateException("UTF-8 no disponible", impossible);
        }
    }

    public static final class ResearchResult {
        public final String question;
        public final List<Source> sources;

        ResearchResult(String question, List<Source> sources) {
            this.question = question;
            this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
        }

        public boolean hasSources() { return !sources.isEmpty(); }

        public String asGroundedContext() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < sources.size(); i++) {
                Source source = sources.get(i);
                result.append('[').append(i + 1).append("] ")
                        .append(source.title).append('\n')
                        .append(source.extract).append('\n')
                        .append("URL: ").append(source.url).append("\n\n");
            }
            return result.toString().trim();
        }
    }

    public static final class Source {
        public final String title;
        public final String url;
        public final String extract;

        Source(String title, String url, String extract) {
            this.title = title;
            this.url = url;
            this.extract = extract;
        }
    }
}
