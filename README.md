# GitHub Ranking

GitHub star ranking for users, organizations and repositories.

https://github-ranking.com

## Build Status

| Server | Worker |
|:-------|:-------|
| [![Build Status](https://travis-ci.org/k0kubun/github-ranking.svg?branch=master)](https://travis-ci.org/k0kubun/github-ranking) | [![wercker status](https://app.wercker.com/status/ce0c5e8138d8a2aef9eb7b0466ae98b8/s/master "wercker status")](https://app.wercker.com/project/byKey/ce0c5e8138d8a2aef9eb7b0466ae98b8) |

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
