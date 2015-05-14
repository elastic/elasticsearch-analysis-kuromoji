Japanese (kuromoji) Analysis for Elasticsearch
==================================

The Japanese (kuromoji) Analysis plugin integrates Lucene kuromoji analysis module into elasticsearch.

In order to install the plugin, run: 

```sh
bin/plugin install elasticsearch/elasticsearch-analysis-kuromoji/2.5.0
```

You need to install a version matching your Elasticsearch version:

| elasticsearch |  Kuromoji Analysis Plugin   |   Docs     |  
|---------------|-----------------------------|------------|
| master        |  Build from source          | See below  |
| es-1.x        |  Build from source          | [2.6.0-SNAPSHOT](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/es-1.x/#version-260-snapshot-for-elasticsearch-1x)  |
|    es-1.5              |     2.5.0         | [2.5.0](https://github.com/elastic/elasticsearch-analysis-kuromoji/tree/v2.5.0/#version-250-for-elasticsearch-15)                  |
|    es-1.4              |     2.4.3         | [2.4.3](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v2.4.3/#version-243-for-elasticsearch-14)                  |
| < 1.4.5       |  2.4.2                      | [2.4.2](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v2.4.2/#version-242-for-elasticsearch-14)                  |
| < 1.4.3       |  2.4.1                      | [2.4.1](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v2.4.1/#version-241-for-elasticsearch-14)                  |
| es-1.3        |  2.3.0                      | [2.3.0](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v2.3.0/#japanese-kuromoji-analysis-for-elasticsearch)  |
| es-1.2        |  2.2.0                      | [2.2.0](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v2.2.0/#japanese-kuromoji-analysis-for-elasticsearch)  |
| es-1.1        |  2.1.0                      | [2.1.0](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v2.1.0/#japanese-kuromoji-analysis-for-elasticsearch)  |
| es-1.0        |  2.0.0                      | [2.0.0](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v2.0.0/#japanese-kuromoji-analysis-for-elasticsearch)  |
| es-0.90       |  1.8.0                      | [1.8.0](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji/tree/v1.8.0/#japanese-kuromoji-analysis-for-elasticsearch)  |

To build a `SNAPSHOT` version, you need to build it with Maven:

```bash
mvn clean install
plugin --install analysis-kuromoji \
       --url file:target/releases/elasticsearch-analysis-kuromoji-X.X.X-SNAPSHOT.zip
```

Includes Analyzer, Tokenizer, TokenFilter, CharFilter
-----------------------------------------------

The plugin includes these analyzer and tokenizer, tokenfilter.

| name                    | type        |
|-------------------------|-------------|
| kuromoji_iteration_mark | charfilter  |
| kuromoji                | analyzer    |
| kuromoji_tokenizer      | tokenizer   |
| kuromoji_baseform       | tokenfilter |
| kuromoji_part_of_speech | tokenfilter |
| kuromoji_readingform    | tokenfilter |
| kuromoji_stemmer        | tokenfilter |
| ja_stop                 | tokenfilter |


Usage
-----

## Analyzer : kuromoji

An analyzer of type `kuromoji`.
This analyzer is the following tokenizer and tokenfilter combination.

* `kuromoji_tokenizer` : Kuromoji Tokenizer
* `kuromoji_baseform` : Kuromoji BasicFormFilter (TokenFilter)
* `kuromoji_part_of_speech` : Kuromoji Part of Speech Stop Filter (TokenFilter)
* `cjk_width` : CJK Width Filter (TokenFilter)
* `stop` : Stop Filter (TokenFilter)
* `kuromoji_stemmer` : Kuromoji Katakana Stemmer Filter(TokenFilter)
* `lowercase` : LowerCase Filter (TokenFilter)

## CharFilter : kuromoji_iteration_mark

A charfilter of type `kuromoji_iteration_mark`.
This charfilter is Normalizes Japanese horizontal iteration marks (odoriji) to their expanded form.

The following ar setting that can be set for a `kuromoji_iteration_mark` charfilter type:

| **Setting**     | **Description**                                              | **Default value** |
|:----------------|:-------------------------------------------------------------|:------------------|
| normalize_kanji | indicates whether kanji iteration marks should be normalized | `true`            |
| normalize_kana  | indicates whether kanji iteration marks should be normalized | `true`            |

## Tokenizer : kuromoji_tokenizer

A tokenizer of type `kuromoji_tokenizer`.

The following are settings that can be set for a `kuromoji_tokenizer` tokenizer type:

| **Setting**         | **Description**                                                                                                           | **Default value** |
|:--------------------|:--------------------------------------------------------------------------------------------------------------------------|:------------------|
| mode                | Tokenization mode: this determines how the tokenizer handles compound and unknown words. `normal` and `search`, `extended`| `search`          |
| discard_punctuation | `true` if punctuation tokens should be dropped from the output.                                                           | `true`            |
| user_dictionary     | set User Dictionary file                                                                                                  |                   |

### Tokenization mode

The mode is three types.

* `normal` : Ordinary segmentation: no decomposition for compounds

* `search` : Segmentation geared towards search: this includes a decompounding process for long nouns, also including the full compound token as a synonym.

* `extended` : Extended mode outputs unigrams for unknown words.

#### Difference tokenization mode outputs

Input text is `関西国際空港` and `アブラカダブラ`.

| **mode**   | `関西国際空港` | `アブラカダブラ` |
|:-----------|:-------------|:-------|
| `normal`   | `関西国際空港` | `アブラカダブラ` |
| `search`   | `関西` `関西国際空港` `国際` `空港` | `アブラカダブラ` |
| `extended` | `関西` `国際` `空港` | `ア` `ブ` `ラ` `カ` `ダ` `ブ` `ラ` |

### User Dictionary

Kuromoji tokenizer use MeCab-IPADIC dictionary by default.
And Kuromoji is added an entry of dictionary to define by user; this is User Dictionary.
User Dictionary entries are defined using the following CSV format:

```
<text>,<token 1> ... <token n>,<reading 1> ... <reading n>,<part-of-speech tag>
```

Dictionary Example

```
東京スカイツリー,東京 スカイツリー,トウキョウ スカイツリー,カスタム名詞
```

To use User Dictionary set file path to `user_dict` attribute.
User Dictionary file is placed `ES_HOME/config` directory.

### example

_Example Settings:_

```sh
curl -XPUT 'http://localhost:9200/kuromoji_sample/' -d'
{
    "settings": {
        "index":{
            "analysis":{
                "tokenizer" : {
                    "kuromoji_user_dict" : {
                       "type" : "kuromoji_tokenizer",
                       "mode" : "extended",
                       "discard_punctuation" : "false",
                       "user_dictionary" : "userdict_ja.txt"
                    }
                },
                "analyzer" : {
                    "my_analyzer" : {
                        "type" : "custom",
                        "tokenizer" : "kuromoji_user_dict"
                    }
                }

            }
        }
    }
}
'
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=my_analyzer&pretty' -d '東京スカイツリー'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "東京",
    "start_offset" : 0,
    "end_offset" : 2,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "スカイツリー",
    "start_offset" : 2,
    "end_offset" : 8,
    "type" : "word",
    "position" : 2
  } ]
}
```

## TokenFilter : kuromoji_baseform

A token filter of type `kuromoji_baseform` that replaces term text with BaseFormAttribute.
This acts as a lemmatizer for verbs and adjectives.

### example

_Example Settings:_

```sh
curl -XPUT 'http://localhost:9200/kuromoji_sample/' -d'
{
    "settings": {
        "index":{
            "analysis":{
                "analyzer" : {
                    "my_analyzer" : {
                        "tokenizer" : "kuromoji_tokenizer",
                        "filter" : ["kuromoji_baseform"]
                    }
                }
            }
        }
    }
}
'
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=my_analyzer&pretty' -d '飲み'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "飲む",
    "start_offset" : 0,
    "end_offset" : 2,
    "type" : "word",
    "position" : 1
  } ]
}
```

## TokenFilter : kuromoji_part_of_speech

A token filter of type `kuromoji_part_of_speech` that removes tokens that match a set of part-of-speech tags.

The following are settings that can be set for a stop token filter type:

| **Setting** | **Description**                                      |
|:------------|:-----------------------------------------------------|
| stoptags    | A list of part-of-speech tags that should be removed |

Note that default setting is stoptags.txt include lucene-analyzer-kuromoji.jar.

### example

_Example Settings:_

```sh
curl -XPUT 'http://localhost:9200/kuromoji_sample/' -d'
{
    "settings": {
        "index":{
            "analysis":{
                "analyzer" : {
                    "my_analyzer" : {
                        "tokenizer" : "kuromoji_tokenizer",
                        "filter" : ["my_posfilter"]
                    }
                },
                "filter" : {
                    "my_posfilter" : {
                        "type" : "kuromoji_part_of_speech",
                        "stoptags" : [
                            "助詞-格助詞-一般",
                            "助詞-終助詞"
                        ]
                    }
                }
            }
        }
    }
}
'
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=my_analyzer&pretty' -d '寿司がおいしいね'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "寿司",
    "start_offset" : 0,
    "end_offset" : 2,
    "type" : "word",
    "position" : 1
  }, {
    "token" : "おいしい",
    "start_offset" : 3,
    "end_offset" : 7,
    "type" : "word",
    "position" : 3
  } ]
}
```

## TokenFilter : kuromoji_readingform

A token filter of type `kuromoji_readingform` that replaces the term attribute with the reading of a token in either katakana or romaji form.
The default reading form is katakana.

The following are settings that can be set for a `kuromoji_readingform` token filter type:

| **Setting** | **Description**                                           | **Default value** |
|:------------|:----------------------------------------------------------|:------------------|
| use_romaji  | `true` if romaji reading form output instead of katakana. | `false`           |

Note that elasticsearch-analysis-kuromoji built-in `kuromoji_readingform` set default `true` to `use_romaji` attribute.

### example

_Example Settings:_

```sh
curl -XPUT 'http://localhost:9200/kuromoji_sample/' -d'
{
    "settings": {
        "index":{
            "analysis":{
                "analyzer" : {
                    "romaji_analyzer" : {
                        "tokenizer" : "kuromoji_tokenizer",
                        "filter" : ["romaji_readingform"]
                    },
                    "katakana_analyzer" : {
                        "tokenizer" : "kuromoji_tokenizer",
                        "filter" : ["katakana_readingform"]
                    }
                },
                "filter" : {
                    "romaji_readingform" : {
                        "type" : "kuromoji_readingform",
                        "use_romaji" : true
                    },
                    "katakana_readingform" : {
                        "type" : "kuromoji_readingform",
                        "use_romaji" : false
                    }
                }
            }
        }
    }
}
'
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=katakana_analyzer&pretty' -d '寿司'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "スシ",
    "start_offset" : 0,
    "end_offset" : 2,
    "type" : "word",
    "position" : 1
  } ]
}
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=romaji_analyzer&pretty' -d '寿司'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "sushi",
    "start_offset" : 0,
    "end_offset" : 2,
    "type" : "word",
    "position" : 1
  } ]
}
```

## TokenFilter : kuromoji_stemmer

A token filter of type `kuromoji_stemmer` that normalizes common katakana spelling variations ending in a long sound character by removing this character (U+30FC).
Only katakana words longer than a minimum length are stemmed (default is four).

Note that only full-width katakana characters are supported.

The following are settings that can be set for a `kuromoji_stemmer` token filter type:

| **Setting**     | **Description**            | **Default value** |
|:----------------|:---------------------------|:------------------|
| minimum_length  | The minimum length to stem | `4`               |

### example

_Example Settings:_

```sh
curl -XPUT 'http://localhost:9200/kuromoji_sample/' -d'
{
    "settings": {
        "index":{
            "analysis":{
                "analyzer" : {
                    "my_analyzer" : {
                        "tokenizer" : "kuromoji_tokenizer",
                        "filter" : ["my_katakana_stemmer"]
                    }
                },
                "filter" : {
                    "my_katakana_stemmer" : {
                        "type" : "kuromoji_stemmer",
                        "minimum_length" : 4
                    }
                }
            }
        }
    }
}
'
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=my_analyzer&pretty' -d 'コピー'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "コピー",
    "start_offset" : 0,
    "end_offset" : 3,
    "type" : "word",
    "position" : 1
  } ]
}
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=my_analyzer&pretty' -d 'サーバー'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "サーバ",
    "start_offset" : 0,
    "end_offset" : 4,
    "type" : "word",
    "position" : 1
  } ]
}
```


## TokenFilter : ja_stop


A token filter of type `ja_stop` that provide a predefined "_japanese_" stop words.
*Note: It is only provide "_japanese_". If you want to use other predefined stop words, you can use `stop` token filter.*

_Example Settings:_

### example

```sh
curl -XPUT 'http://localhost:9200/kuromoji_sample/' -d'
{
    "settings": {
        "index":{
            "analysis":{
                "analyzer" : {
                    "analyzer_with_ja_stop" : {
                        "tokenizer" : "kuromoji_tokenizer",
                        "filter" : ["ja_stop"]
                    }
                },
                "filter" : {
                    "ja_stop" : {
                        "type" : "ja_stop",
                        "stopwords" : ["_japanese_", "ストップ"]
                    }
                }
            }
        }
    }
}'
```

_Example Request using `_analyze` API :_

```sh
curl -XPOST 'http://localhost:9200/kuromoji_sample/_analyze?analyzer=katakana_analyzer&pretty' -d 'ストップは消える'
```

_Response :_

```json
{
  "tokens" : [ {
    "token" : "消える",
    "start_offset" : 5,
    "end_offset" : 8,
    "type" : "word",
    "position" : 3
  } ]
}
```

License
-------

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2014 Elasticsearch <http://www.elasticsearch.org>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
