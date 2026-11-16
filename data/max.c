#include "similarity.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>

/* ---------- Utilitaire : min de 3 entiers ---------- */
static int min3(int a, int b, int c) {
    int m = a < b ? a : b;
    return m < c ? m : c;
}

/* ---------- Levenshtein (distance d'édition) ---------- */
static int levenshtein_distance(const char *s1, const char *s2) {
    int n = (int)strlen(s1), m = (int)strlen(s2);
    int *prev = malloc((m + 1) * sizeof(int));
    int *cur  = malloc((m + 1) * sizeof(int));
    for (int j = 0; j <= m; j++) prev[j] = j;

    for (int i = 1; i <= n; i++) {
        cur[0] = i;
        for (int j = 1; j <= m; j++) {
            int cost = (s1[i-1] == s2[j-1]) ? 0 : 1;
            cur[j] = min3(prev[j] + 1,        /* suppression */
                          cur[j-1] + 1,        /* insertion   */
                          prev[j-1] + cost);   /* substitution */
        }
        int *tmp = prev; prev = cur; cur = tmp;
    }
    int d = prev[m];
    free(prev);
    free(cur);
    return d;
}

double levenshtein_similarity(const char *s1, const char *s2) {
    int n = (int)strlen(s1), m = (int)strlen(s2);
    if (n == 0 && m == 0) return 1.0;
    int max = n > m ? n : m;
    return 1.0 - (double)levenshtein_distance(s1, s2) / max;
}

/* ---------- Jaccard sur tokens ---------- */
/* On segmente en mots alphanumériques, puis on compare les ensembles. */

#define MAX_TOKENS 4096
#define MAX_TOKEN_LEN 64

static int tokenize(const char *s, char tokens[MAX_TOKENS][MAX_TOKEN_LEN]) {
    int count = 0, k = 0;
    for (int i = 0; ; i++) {
        unsigned char c = (unsigned char)s[i];
        if (isalnum(c) || c == '_') {
            if (k < MAX_TOKEN_LEN - 1) tokens[count][k++] = (char)tolower(c);
        } else {
            if (k > 0) { tokens[count][k] = '\0'; count++; k = 0; }
            if (c == '\0' || count >= MAX_TOKENS) break;
        }
    }
    return count;
}

double jaccard_similarity(const char *s1, const char *s2) {
    char t1[MAX_TOKENS][MAX_TOKEN_LEN];
    char t2[MAX_TOKENS][MAX_TOKEN_LEN];
    int n1 = tokenize(s1, t1);
    int n2 = tokenize(s2, t2);
    if (n1 == 0 && n2 == 0) return 1.0;

    int inter = 0;
    char used[MAX_TOKENS] = {0};
    for (int i = 0; i < n1; i++) {
        for (int j = 0; j < n2; j++) {
            if (!used[j] && strcmp(t1[i], t2[j]) == 0) {
                used[j] = 1; inter++; break;
            }
        }
    }
    int uni = n1 + n2 - inter;
    return uni == 0 ? 0.0 : (double)inter / uni;
}

/* ---------- Cosinus sur fréquences ASCII ---------- */
double cosine_similarity(const char *s1, const char *s2) {
    int f1[256] = {0}, f2[256] = {0};
    for (int i = 0; s1[i]; i++) f1[(unsigned char)tolower((unsigned char)s1[i])]++;
    for (int i = 0; s2[i]; i++) f2[(unsigned char)tolower((unsigned char)s2[i])]++;

    double dot = 0, n1 = 0, n2 = 0;
    for (int i = 0; i < 256; i++) {
        dot += (double)f1[i] * f2[i];
        n1  += (double)f1[i] * f1[i];
        n2  += (double)f2[i] * f2[i];
    }
    if (n1 == 0 || n2 == 0) return (n1 == n2) ? 1.0 : 0.0;
    return dot / (sqrt(n1) * sqrt(n2));
}

/* ---------- Plus longue sous-chaîne commune ---------- */
double longest_common_substring(const char *s1, const char *s2) {
    int n = (int)strlen(s1), m = (int)strlen(s2);
    if (n == 0 || m == 0) return 0.0;

    int *prev = calloc(m + 1, sizeof(int));
    int *cur  = calloc(m + 1, sizeof(int));
    int best = 0;

    for (int i = 1; i <= n; i++) {
        for (int j = 1; j <= m; j++) {
            if (s1[i-1] == s2[j-1]) {
                cur[j] = prev[j-1] + 1;
                if (cur[j] > best) best = cur[j];
            } else cur[j] = 0;
        }
        int *tmp = prev; prev = cur; cur = tmp;
        memset(cur, 0, (m + 1) * sizeof(int));
    }
    free(prev);
    free(cur);
    int max = n > m ? n : m;
    return (double)best / max;
}

/* ---------- Score combiné ---------- */
double combined_similarity(const char *s1, const char *s2) {
    double l = levenshtein_similarity(s1, s2);
    double j = jaccard_similarity(s1, s2);
    double c = cosine_similarity(s1, s2);
    double s = longest_common_substring(s1, s2);
    /* pondération : les tokens pèsent plus lourd */
    return 0.30 * l + 0.35 * j + 0.20 * c + 0.15 * s;
}

/* ---------- Lecture de fichier ---------- */
char *read_file(const char *path) {
    FILE *f = fopen(path, "rb");
    if (!f) return NULL;
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);
    if (sz < 0) { fclose(f); return NULL; }

    char *buf = malloc((size_t)sz + 1);
    if (!buf) { fclose(f); return NULL; }
    size_t r = fread(buf, 1, (size_t)sz, f);
    buf[r] = '\0';
    fclose(f);
    return buf;
}