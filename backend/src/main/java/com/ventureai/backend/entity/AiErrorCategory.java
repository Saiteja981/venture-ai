package com.ventureai.backend.entity;

/**
 * Coarse classification of AI-service failures. Never exposes stack traces
 * or upstream internals to API clients.
 */
public enum AiErrorCategory {
    VALIDATION,
    MODEL_UNAVAILABLE,
    TIMEOUT,
    NETWORK,
    UPSTREAM_ERROR,
    INTERNAL
}