def max_tab(t):
    m = t[0]
    for i in range(1, len(t)):
        if t[i] > m:
            m = t[i]
    return m