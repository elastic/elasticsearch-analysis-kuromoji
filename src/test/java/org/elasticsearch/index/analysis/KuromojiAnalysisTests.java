/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.plugin.analysis.kuromoji.AnalysisKuromojiPlugin;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.hamcrest.Matchers.*;

/**
 */
public class KuromojiAnalysisTests extends ElasticsearchTestCase {

    @Test
    public void testDefaultsKuromojiAnalysis() throws IOException {
        AnalysisService analysisService = createAnalysisService();

        TokenizerFactory tokenizerFactory = analysisService.tokenizer("kuromoji_tokenizer");
        assertThat(tokenizerFactory, instanceOf(KuromojiTokenizerFactory.class));

        TokenFilterFactory filterFactory = analysisService.tokenFilter("kuromoji_part_of_speech");
        assertThat(filterFactory, instanceOf(KuromojiPartOfSpeechFilterFactory.class));

        filterFactory = analysisService.tokenFilter("kuromoji_readingform");
        assertThat(filterFactory, instanceOf(KuromojiReadingFormFilterFactory.class));

        filterFactory = analysisService.tokenFilter("kuromoji_baseform");
        assertThat(filterFactory, instanceOf(KuromojiBaseFormFilterFactory.class));

        filterFactory = analysisService.tokenFilter("kuromoji_stemmer");
        assertThat(filterFactory, instanceOf(KuromojiKatakanaStemmerFactory.class));

        filterFactory = analysisService.tokenFilter("ja_stop");
        assertThat(filterFactory, instanceOf(JapaneseStopTokenFilterFactory.class));

        NamedAnalyzer analyzer = analysisService.analyzer("kuromoji");
        assertThat(analyzer.analyzer(), instanceOf(JapaneseAnalyzer.class));

        analyzer = analysisService.analyzer("my_analyzer");
        assertThat(analyzer.analyzer(), instanceOf(CustomAnalyzer.class));
        assertThat(analyzer.analyzer().tokenStream(null, new StringReader("")), instanceOf(JapaneseTokenizer.class));

        CharFilterFactory  charFilterFactory = analysisService.charFilter("kuromoji_iteration_mark");
        assertThat(charFilterFactory, instanceOf(KuromojiIterationMarkCharFilterFactory.class));

    }

    @Test
    public void testBaseFormFilterFactory() throws IOException {
        AnalysisService analysisService = createAnalysisService();
        TokenFilterFactory tokenFilter = analysisService.tokenFilter("kuromoji_pos");
        assertThat(tokenFilter, instanceOf(KuromojiPartOfSpeechFilterFactory.class));
        String source = "私は制限スピードを超える。";
        String[] expected = new String[]{"私", "は", "制限", "スピード", "を"};
        Tokenizer tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.SEARCH);
        tokenizer.setReader(new StringReader(source));
        assertSimpleTSOutput(tokenFilter.create(tokenizer), expected);
    }

    @Test
    public void testReadingFormFilterFactory() throws IOException {
        AnalysisService analysisService = createAnalysisService();
        TokenFilterFactory tokenFilter = analysisService.tokenFilter("kuromoji_rf");
        assertThat(tokenFilter, instanceOf(KuromojiReadingFormFilterFactory.class));
        String source = "今夜はロバート先生と話した";
        String[] expected_tokens_romanji = new String[]{"kon'ya", "ha", "robato", "sensei", "to", "hanashi", "ta"};

        Tokenizer tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.SEARCH);
        tokenizer.setReader(new StringReader(source));

        assertSimpleTSOutput(tokenFilter.create(tokenizer), expected_tokens_romanji);

        tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.SEARCH);
        tokenizer.setReader(new StringReader(source));
        String[] expected_tokens_katakana = new String[]{"コンヤ", "ハ", "ロバート", "センセイ", "ト", "ハナシ", "タ"};
        tokenFilter = analysisService.tokenFilter("kuromoji_readingform");
        assertThat(tokenFilter, instanceOf(KuromojiReadingFormFilterFactory.class));
        assertSimpleTSOutput(tokenFilter.create(tokenizer), expected_tokens_katakana);
    }

    @Test
    public void testKatakanaStemFilter() throws IOException {
        AnalysisService analysisService = createAnalysisService();
        TokenFilterFactory tokenFilter = analysisService.tokenFilter("kuromoji_stemmer");
        assertThat(tokenFilter, instanceOf(KuromojiKatakanaStemmerFactory.class));
        String source = "明後日パーティーに行く予定がある。図書館で資料をコピーしました。";

        Tokenizer tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.SEARCH);
        tokenizer.setReader(new StringReader(source));

        // パーティー should be stemmed by default
        // (min len) コピー should not be stemmed
        String[] expected_tokens_katakana = new String[]{"明後日", "パーティ", "に", "行く", "予定", "が", "ある", "図書館", "で", "資料", "を", "コピー", "し", "まし", "た"};
        assertSimpleTSOutput(tokenFilter.create(tokenizer), expected_tokens_katakana);

        tokenFilter = analysisService.tokenFilter("kuromoji_ks");
        assertThat(tokenFilter, instanceOf(KuromojiKatakanaStemmerFactory.class));
        tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.SEARCH);
        tokenizer.setReader(new StringReader(source));

        // パーティー should not be stemmed since min len == 6
        // コピー should not be stemmed
        expected_tokens_katakana = new String[]{"明後日", "パーティー", "に", "行く", "予定", "が", "ある", "図書館", "で", "資料", "を", "コピー", "し", "まし", "た"};
        assertSimpleTSOutput(tokenFilter.create(tokenizer), expected_tokens_katakana);
    }
    @Test
    public void testIterationMarkCharFilter() throws IOException {
        AnalysisService analysisService = createAnalysisService();
        // test only kanji
        CharFilterFactory charFilterFactory = analysisService.charFilter("kuromoji_im_only_kanji");
        assertNotNull(charFilterFactory);
        assertThat(charFilterFactory, instanceOf(KuromojiIterationMarkCharFilterFactory.class));

        String source = "ところゞゝゝ、ジヾが、時々、馬鹿々々しい";
        String expected = "ところゞゝゝ、ジヾが、時時、馬鹿馬鹿しい";

        assertCharFilterEquals(charFilterFactory.create(new StringReader(source)), expected);

        // test only kana

        charFilterFactory = analysisService.charFilter("kuromoji_im_only_kana");
        assertNotNull(charFilterFactory);
        assertThat(charFilterFactory, instanceOf(KuromojiIterationMarkCharFilterFactory.class));

        expected = "ところどころ、ジジが、時々、馬鹿々々しい";

        assertCharFilterEquals(charFilterFactory.create(new StringReader(source)), expected);

        // test default

        charFilterFactory = analysisService.charFilter("kuromoji_im_default");
        assertNotNull(charFilterFactory);
        assertThat(charFilterFactory, instanceOf(KuromojiIterationMarkCharFilterFactory.class));

        expected = "ところどころ、ジジが、時時、馬鹿馬鹿しい";

        assertCharFilterEquals(charFilterFactory.create(new StringReader(source)), expected);
    }

    @Test
    public void testJapaneseStopFilterFactory() throws IOException {
        AnalysisService analysisService = createAnalysisService();
        TokenFilterFactory tokenFilter = analysisService.tokenFilter("ja_stop");
        assertThat(tokenFilter, instanceOf(JapaneseStopTokenFilterFactory.class));
        String source = "私は制限スピードを超える。";
        String[] expected = new String[]{"私", "制限", "超える"};
        Tokenizer tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.SEARCH);
        tokenizer.setReader(new StringReader(source));
        assertSimpleTSOutput(tokenFilter.create(tokenizer), expected);
    }


    public AnalysisService createAnalysisService() {
        Settings settings = ImmutableSettings.settingsBuilder()
                .loadFromClasspath("org/elasticsearch/index/analysis/kuromoji_analysis.json")
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .build();

        Index index = new Index("test");

        Injector parentInjector = new ModulesBuilder().add(new SettingsModule(settings),
                new EnvironmentModule(new Environment(settings)),
                new IndicesAnalysisModule())
                .createInjector();

        AnalysisModule analysisModule = new AnalysisModule(settings, parentInjector.getInstance(IndicesAnalysisService.class));
        new AnalysisKuromojiPlugin().onModule(analysisModule);

        Injector injector = new ModulesBuilder().add(
                new IndexSettingsModule(index, settings),
                new IndexNameModule(index),
                analysisModule)
                .createChildInjector(parentInjector);

        return injector.getInstance(AnalysisService.class);
    }

    public static void assertSimpleTSOutput(TokenStream stream,
                                            String[] expected) throws IOException {
        stream.reset();
        CharTermAttribute termAttr = stream.getAttribute(CharTermAttribute.class);
        assertThat(termAttr, notNullValue());
        int i = 0;
        while (stream.incrementToken()) {
            assertThat(expected.length, greaterThan(i));
            assertThat( "expected different term at index " + i, expected[i++], equalTo(termAttr.toString()));
        }
        assertThat("not all tokens produced", i, equalTo(expected.length));
    }

    private void assertCharFilterEquals(Reader filtered,
                                        String expected) throws IOException {
        String actual = readFully(filtered);
        assertThat(actual, equalTo(expected));
    }

    private String readFully(Reader reader) throws IOException {
        StringBuilder buffer = new StringBuilder();
        int ch;
        while((ch = reader.read()) != -1){
            buffer.append((char)ch);
        }
        return buffer.toString();
    }

}
