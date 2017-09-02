# Gitstar Ranking

GitHub star ranking for users, organizations and repositories.

https://github-ranking.com

## Build Status

| Server | Worker |
|:-------|:-------|
| [![Build Status](https://travis-ci.org/k0kubun/gitstar-ranking.svg?branch=master)](https://travis-ci.org/k0kubun/gitstar-ranking) | [![wercker status](https://app.wercker.com/status/4f1170bbabcfa8b28e379746dff4e167/s/master "wercker status")](https://app.wercker.com/project/byKey/4f1170bbabcfa8b28e379746dff4e167) |

## Screen Shot

[![](http://i.gyazo.com/c737a76774886f59ba73215c01e5d053.png)](http://githubranking.com)

## Development

### Prerequisites

Install following dependencies.

- Ruby 2.4.1
- Java 1.8.0
- MySQL 5.7

Then, execute following command.

```
bin/setup
```

### Run servers

```sh
foreman start
```

## Requirements

- MySQL's time\_zone must be UTC.
