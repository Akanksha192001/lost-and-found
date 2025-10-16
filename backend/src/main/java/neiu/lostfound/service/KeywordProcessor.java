package neiu.lostfound.service;

import neiu.lostfound.config.StopWordsProvider;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.event.SpellChecker;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.dictionary.Dictionary;
import java.io.File;

@Component
public class KeywordProcessor {
    private static final Logger log = LoggerFactory.getLogger(KeywordProcessor.class);
    private final StopWordsProvider stopWordsProvider;
    private final SpellChecker spellChecker;
    private final Dictionary wordNetDictionary;
    private final ResourceLoader resourceLoader;

    @Autowired
    public KeywordProcessor(StopWordsProvider stopWordsProvider, ResourceLoader resourceLoader) {
        this.stopWordsProvider = stopWordsProvider;
        this.resourceLoader = resourceLoader;
        SpellChecker sc = null;
        Dictionary wnd = null;
        try {
            var dictResource = resourceLoader.getResource("classpath:dict/english.0");
            if (dictResource.exists()) {
                SpellDictionary dict = new SpellDictionaryHashMap(dictResource.getFile());
                sc = new SpellChecker(dict);
            } else {
                log.warn("Jazzy dictionary file dict/english.0 not found. Spell correction will be disabled.");
            }
            // JWNL WordNet initialization (fallback if not present)
            try {
                wnd = Dictionary.getDefaultResourceInstance();
            } catch (Exception e) {
                log.warn("WordNet dictionary not found or failed to initialize. Synonym expansion will be disabled.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize spell checker or WordNet: {}", e.getMessage());
        }
        this.spellChecker = sc;
        this.wordNetDictionary = wnd;
    }

    /**
     * Process input text to extract corrected and synonym-expanded keywords
     */
    public Set<String> process(String... texts) {
        Set<String> stopwords = stopWordsProvider.getStopwords();
        Set<String> keywords = Arrays.stream(texts)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .flatMap(t -> Arrays.stream(t.split("\\W+")))
                .filter(w -> w.length() > 2 && !stopwords.contains(w))
                .map(this::spellCorrect)
                .collect(Collectors.toSet());

        Set<String> expanded = new HashSet<>(keywords);
        for (String k : keywords) {
            expanded.addAll(getSynonyms(k));
        }
        return expanded;
    }

    /** Correct spelling of a word using Jazzy */
    private String spellCorrect(String word) {
        if (spellChecker == null) return word;
        List<?> suggestions = spellChecker.getSuggestions(word, 2);
        if (!suggestions.isEmpty()) {
            return suggestions.get(0).toString();
        }
        return word;
    }

    /** Get synonyms from WordNet */
    private Set<String> getSynonyms(String word) {
        Set<String> result = new HashSet<>();
        if (wordNetDictionary == null) return result;
        try {
            IndexWord indexWord = wordNetDictionary.lookupIndexWord(POS.NOUN, word);
            if (indexWord == null) return result;
            for (Synset synset : indexWord.getSenses()) {
                for (Word w : synset.getWords()) {
                    result.add(w.getLemma().replace('_', ' '));
                }
            }
        } catch (Exception e) {
            // Log or handle errors
        }
        return result;
    }
}
