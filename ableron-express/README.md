# @ableron/express

[![Build Status](https://github.com/ableron/ableron/actions/workflows/ableron-express.yml/badge.svg)](https://github.com/ableron/ableron/actions/workflows/ableron-express.yml)
[![npm version](https://badge.fury.io/js/@ableron%2Fexpress.svg)](https://badge.fury.io/js/@ableron%2Fexpress)
[![Node.js Version](https://img.shields.io/badge/Node.js-18+-4EB1BA.svg)](https://nodejs.org/docs/latest-v18.x/api/)

Express Middleware for Ableron Decentralized Server-Side UI Composition.

## Installation

```shell
npm i @ableron/express
```

## Usage

Full example using **ES Modules**

```js
import express from 'express';
import ableron from '@ableron/express';

const app = express();

app.use(
  ableron(
    // custom settings (optional)
    {
      requestTimeoutMs: 5000,
      requestHeadersForward: ['X-Tracking-ID'],
      requestHeadersForwardVary: ['X-Test-Groups'],
      statsAppendToContent: true
      // ...
    },
    // custom logger (optional). If no logger is provided, no logging happens at all
    yourLogger() || console
  )
);
```

Minimal example using **CommonJS**

```js
const express = require('express');
const ableron = require('@ableron/express').default;

const app = express();

app.use(ableron());
```

### Configuration

Configuration options see [@ableron/ableron](https://github.com/ableron/ableron/blob/main/ableron-js/README.md#configuration)
