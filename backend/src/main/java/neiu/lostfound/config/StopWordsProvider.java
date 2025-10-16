package neiu.lostfound.config;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StopWordsProvider implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(StopWordsProvider.class);
    private Set<String> stopwords = new HashSet<>();

    public Set<String> getStopwords() {
        return stopwords;
    }

    @Override
    public void afterPropertiesSet() {
        loadStopwords();
    }

    public void loadStopwords() {
        var stream = getClass().getClassLoader().getResourceAsStream("stopwords.txt");
        if (stream == null) {
            log.warn("stopwords.txt resource not found. Stopwords will be empty.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toLowerCase();
                if (!word.isEmpty()) stopwords.add(word);
            }
        } catch (Exception e) {
            log.error("Could not load stopwords: {}", e.getMessage());
        }
    }
}
